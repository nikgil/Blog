package dev.sirnik.blog.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.sirnik.blog.models.Tag;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
