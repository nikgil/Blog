package dev.sirnik.blog.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.models.projections.BlogPostTagRow;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
        select post.id as postId,
               tag.name as name,
               tag.slug as slug
        from BlogPost post
        join post.tags tag
        where post.id in :postIds
        order by post.id, lower(tag.name), tag.name, tag.id
        """)
    List<BlogPostTagRow> findForPostIds(
        @Param("postIds") Collection<Long> postIds);

    Slice<Tag> findAllByOrderByName(Pageable pageable);

    Slice<Tag> findByNameStartingWithIgnoreCaseOrderByNameAsc(
        String prefix,
        Pageable pageable);
}
