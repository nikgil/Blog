package dev.sirnik.blog.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.sirnik.blog.models.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    boolean existsBy();

    @Query("select u from AdminUser u where u.username = :username and u.email = :email and u.activated = false")
    Optional<AdminUser> findInactiveForVerification(
        @Param("username") String username,
        @Param("email") String email
    );

    // Doing query since the method approach makes it way too long
    @Query("select u from AdminUser u where (u.username = :login or u.email = :login) and u.activated = true")
    Optional<AdminUser> findActiveByLogin(@Param("login") String login);

    Optional<AdminUser> findByUsernameOrEmail(
        String username,
        String email
    );

    Optional<AdminUser> findByUsername(String username);

    // Delete entries for all users that are not the current one
    void deleteByUsernameNot(String username);

    @Query("select u.prevLoginAt from AdminUser u where u.username = :username")
    Optional<Instant> findPrevLoginAtByUsername(
        @Param("username") String username
    );
}
