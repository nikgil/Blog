package dev.sirnik.blog.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.projections.BlogPostLink;
import dev.sirnik.blog.models.projections.BlogPostPreview;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    @EntityGraph(attributePaths = "tags")
    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    Slice<BlogPostPreview> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable);

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
            Long id);

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
            Long id);
}
