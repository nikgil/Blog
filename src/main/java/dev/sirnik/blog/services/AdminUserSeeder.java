package dev.sirnik.blog.services;

import java.util.regex.Pattern;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.services.AdminUserSeeder.AdminUserProperties;

@Component
@EnableConfigurationProperties(AdminUserProperties.class)
public class AdminUserSeeder implements ApplicationRunner {
    private static final Pattern BCRYPT_HASH = Pattern
        .compile("\\$2[aby]\\$\\d{2}\\$[./0-9A-Za-z]{53}");
    // $<version>$<cost>$<salt><hash>

    private final AdminUserRepository adminUserRepository;
    private final AdminUserProperties properties;

    public AdminUserSeeder(AdminUserRepository adminUserRepository,
        AdminUserProperties properties) {
        this.adminUserRepository = adminUserRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String username = properties.username();
        String passwordHash = properties.passwordHash();

        if (!StringUtils.hasText(username)
            || !StringUtils.hasText(passwordHash)) {
            return;
        }

        // placeholder
        if (username.contains("${") || passwordHash.contains("${")) {
            throw new IllegalStateException(
                "Set BLOG_ADMIN_USERNAME and BLOG_ADMIN_PASSWORD_HASH;"
                    + " blog.admin.* contains an unresolved placeholder.");
        }

        // Seems I have to do this manually
        if (!BCRYPT_HASH.matcher(passwordHash).matches()) {
            throw new IllegalStateException(
                "blog.admin.password-hash is not a BCrypt hash.");
        }

        // Removing or renaming the admin in configuration revokes the old one.
        adminUserRepository.deleteByUsernameNot(username);

        AdminUser adminUser = adminUserRepository
            .findByUsername(username)
            .orElseGet(() -> new AdminUser(username, passwordHash));
        adminUser.setPasswordHash(passwordHash);
        adminUserRepository.save(adminUser);
    }

    @ConfigurationProperties(prefix = "blog.admin")
    public static record AdminUserProperties(String username,
        String passwordHash) {
    }
}
