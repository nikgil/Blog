package dev.sirnik.blog.models;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "admin_users", uniqueConstraints = @UniqueConstraint(name = "uk_admin_users_username", columnNames = "username"))
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "current_login_at")
    private Instant currentLoginAt;

    @Column(name = "prev_login_at")
    private Instant prevLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean activated;

    protected AdminUser() {
    }

    public AdminUser(
        String email,
        String username,
        String passwordHash
    ) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.activated = false;
    }

    @PrePersist
    void setCreationTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getCurrentLoginAt() {
        return currentLoginAt;
    }

    public Instant getPrevLoginAt() {
        return prevLoginAt;
    }

    public void updateTimeStampsToNow() {
        prevLoginAt = currentLoginAt;
        currentLoginAt = currentTimestamp();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActivated() {
        return activated;
    }

    private static Instant currentTimestamp() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
