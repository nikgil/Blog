package dev.sirnik.blog.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.projections.ArchiveMonth;
import dev.sirnik.blog.models.projections.BlogPostLink;

public interface BlogPostRepository
    extends
        JpaRepository<BlogPost, Long>,
        JpaSpecificationExecutor<BlogPost> {

    // findOne(PredicateSpecification) is a default method that delegates to
    // this one, so the entity graph applies to every predicate-based lookup.
    @Override
    @EntityGraph(attributePaths = "tags")
    Optional<BlogPost> findOne(Specification<BlogPost> spec);

    boolean existsBySlug(String slug);

    @Query("""
        SELECT p.title AS title, p.slug AS slug
        FROM BlogPost p
        WHERE p.published = true
          AND (
              p.createdAt < :createdAt
              OR (p.createdAt = :createdAt AND p.id < :id)
          )
        ORDER BY p.createdAt DESC, p.id DESC
        LIMIT 1
        """)
    BlogPostLink findOlderPublished(
        Instant createdAt,
        Long id
    );

    @Query("""
        SELECT p.title AS title, p.slug AS slug
        FROM BlogPost p
        WHERE p.published = true
          AND (
              p.createdAt > :createdAt
              OR (p.createdAt = :createdAt AND p.id > :id)
          )
        ORDER BY p.createdAt ASC, p.id ASC
        LIMIT 1
        """)
    BlogPostLink findNewerPublished(
        Instant createdAt,
        Long id
    );

    @Query("""
        SELECT
                year(p.createdAt) AS year,
                month(p.createdAt) AS month,
                COUNT(*) AS postCount
        FROM BlogPost p
        WHERE (:includeDrafts = true OR p.published = true)
        GROUP BY
                year(p.createdAt),
                month(p.createdAt)
        ORDER BY year DESC, month DESC
                                """)
    List<ArchiveMonth> findAllBlogPostsMonths(boolean includeDrafts);
}
