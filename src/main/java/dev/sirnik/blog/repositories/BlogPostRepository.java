package dev.sirnik.blog.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.sirnik.blog.models.BlogPost;

import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
