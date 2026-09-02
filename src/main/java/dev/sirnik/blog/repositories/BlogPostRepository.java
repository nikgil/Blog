package dev.sirnik.blog.repositories;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.projections.BlogPostPreview;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Slice<BlogPostPreview> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable);
}
