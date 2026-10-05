package dev.sirnik.blog.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.sirnik.blog.models.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);
}
