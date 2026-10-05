package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.services.AdminUserSeeder;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserSeederTests {

    private final AdminUserRepository adminUserRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    AdminUserSeederTests(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Test
    void blankConfigurationSeedsNothing() {
        seeder("", "").run(null);

        assertThat(adminUserRepository.count()).isZero();
    }

    @Test
    void seedsOneAdminWithConfiguredHash() {
        String hash = encoder.encode("secret");

        seeder("owner", hash).run(null);

        AdminUser admin = adminUserRepository
            .findByUsername("owner")
            .orElseThrow();
        assertThat(admin.getPasswordHash()).isEqualTo(hash);
        assertThat(encoder.matches("secret", admin.getPasswordHash())).isTrue();
        assertThat(adminUserRepository.count()).isEqualTo(1);
    }

    @Test
    void runningTwiceIsIdempotent() {
        String hash = encoder.encode("secret");

        seeder("owner", hash).run(null);
        seeder("owner", hash).run(null);

        assertThat(adminUserRepository.count()).isEqualTo(1);
    }

    @Test
    void changedHashUpdatesTheExistingRowAndKeepsLoginTimes() {
        seeder("owner", encoder.encode("old")).run(null);
        AdminUser admin = adminUserRepository
            .findByUsername("owner")
            .orElseThrow();
        admin.updateTimeStampsToNow();
        admin.updateTimeStampsToNow();
        adminUserRepository.saveAndFlush(admin);
        java.time.Instant current = admin.getCurrentLoginAt();
        java.time.Instant prev = admin.getPrevLoginAt();

        String newHash = encoder.encode("new");
        seeder("owner", newHash).run(null);

        AdminUser updated = adminUserRepository
            .findByUsername("owner")
            .orElseThrow();
        assertThat(updated.getPasswordHash()).isEqualTo(newHash);
        assertThat(updated.getCurrentLoginAt()).isEqualTo(current);
        assertThat(updated.getPrevLoginAt()).isEqualTo(prev);
        assertThat(adminUserRepository.count()).isEqualTo(1);
    }

    @Test
    void renamingTheAdminRemovesTheOldOne() {
        String hash = encoder.encode("secret");
        seeder("old-name", hash).run(null);

        seeder("new-name", hash).run(null);

        assertThat(adminUserRepository.findByUsername("old-name")).isEmpty();
        assertThat(adminUserRepository.findByUsername("new-name")).isPresent();
        assertThat(adminUserRepository.count()).isEqualTo(1);
    }

    @Test
    void plainPasswordInsteadOfHashFailsStartup() {
        assertThatThrownBy(() -> seeder("owner", "not-a-hash").run(null))
            .isInstanceOf(IllegalStateException.class);
        assertThat(adminUserRepository.count()).isZero();
    }

    @Test
    void unresolvedPlaceholderFailsStartupInsteadOfBecomingTheUsername() {
        String hash = encoder.encode("secret");

        assertThatThrownBy(
            () -> seeder("${BLOG_ADMIN_USERNAME}", hash).run(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BLOG_ADMIN_USERNAME");
        assertThatThrownBy(
            () -> seeder("owner", "${BLOG_ADMIN_PASSWORD_HASH}").run(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BLOG_ADMIN_PASSWORD_HASH");
        assertThat(adminUserRepository.count()).isZero();
    }

    private AdminUserSeeder seeder(String username, String passwordHash) {
        return new AdminUserSeeder(adminUserRepository,
            new AdminUserSeeder.AdminUserProperties(username, passwordHash));
    }
}
