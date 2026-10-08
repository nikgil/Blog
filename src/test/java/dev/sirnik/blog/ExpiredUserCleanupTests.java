package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.services.ExpiredUserCleanupRunner;
import dev.sirnik.blog.services.UserRegistrationService;
import dev.sirnik.blog.utils.JsonWebTokenUtils;
import dev.sirnik.blog.utils.JsonWebTokenUtils.JWTPayload;
import jakarta.persistence.EntityManager;

/**
 * The startup sweep removes unapproved users once their approval link has
 * expired (one hour), and nothing else. {@code created_at} has no setter, so
 * each test back-dates the row with a native update after saving it.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpiredUserCleanupTests {

    @Autowired
    private UserRegistrationService registrationService;

    @Autowired
    private ExpiredUserCleanupRunner runner;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void pendingUserOlderThanAnHourIsDeleted() {
        saveUser("stale", false, Duration.ofMinutes(61));

        int removed = registrationService
            .deleteExpiredInactiveUsers(Instant.now());

        assertThat(removed).isEqualTo(1);
        assertThat(adminUserRepository.findByUsername("stale")).isEmpty();
    }

    @Test
    void pendingUserYoungerThanAnHourIsKept() {
        saveUser("fresh", false, Duration.ofMinutes(59));

        int removed = registrationService
            .deleteExpiredInactiveUsers(Instant.now());

        assertThat(removed).isZero();
        assertThat(adminUserRepository.findByUsername("fresh")).isPresent();
    }

    @Test
    void activatedUsersAreNeverDeletedHoweverOld() {
        saveUser("owner", true, Duration.ofDays(365));

        int removed = registrationService
            .deleteExpiredInactiveUsers(Instant.now());

        assertThat(removed).isZero();
        assertThat(adminUserRepository.findByUsername("owner")).isPresent();
    }

    @Test
    void onlyTheExpiredPendingUsersAreRemovedFromAMixedTable() {
        saveUser("stale-one", false, Duration.ofHours(2));
        saveUser("stale-two", false, Duration.ofDays(3));
        saveUser("fresh", false, Duration.ofMinutes(5));
        saveUser("owner", true, Duration.ofDays(30));

        int removed = registrationService
            .deleteExpiredInactiveUsers(Instant.now());

        assertThat(removed).isEqualTo(2);
        assertThat(adminUserRepository.findAll())
            .extracting(AdminUser::getUsername)
            .containsExactlyInAnyOrder("fresh", "owner");
    }

    @Test
    void anEmptyTableIsFine() {
        assertThat(
            registrationService.deleteExpiredInactiveUsers(Instant.now())
        ).isZero();
    }

    @Test
    void theStartupRunnerPerformsTheSweep() throws Exception {
        saveUser("stale", false, Duration.ofHours(5));
        saveUser("fresh", false, Duration.ofMinutes(1));

        runner.run(new DefaultApplicationArguments());

        assertThat(adminUserRepository.findAll())
            .extracting(AdminUser::getUsername)
            .containsExactly("fresh");
    }

    @Test
    void aUserIsDeletedExactlyWhenTheirApprovalLinkStopsVerifying() {
        // Same one-hour window on both sides: a user the sweep keeps can still
        // be approved, and a user the sweep deletes could no longer be.
        saveUser("fresh", false, Duration.ofMinutes(59));
        saveUser("stale", false, Duration.ofMinutes(61));
        long now = System.currentTimeMillis();

        assertThat(canStillBeApproved("fresh", now)).isTrue();
        assertThat(canStillBeApproved("stale", now)).isFalse();

        registrationService.deleteExpiredInactiveUsers(Instant.now());

        assertThat(adminUserRepository.findByUsername("fresh")).isPresent();
        assertThat(adminUserRepository.findByUsername("stale")).isEmpty();
    }

    private boolean canStillBeApproved(
        String username,
        long nowMillis
    ) {
        AdminUser user = adminUserRepository
            .findByUsername(username)
            .orElseThrow();

        return JsonWebTokenUtils
            .verifyPayload(
                new JWTPayload(
                    user.getUsername(),
                    user.getEmail(),
                    user.getCreatedAt().toEpochMilli()
                ), adminUserRepository, nowMillis
            );
    }

    private void saveUser(
        String username,
        boolean activated,
        Duration age
    ) {
        AdminUser user = new AdminUser(
            username + "@example.com",
            username,
            "{noop}unused-hash"
        );
        user.setActivated(activated);
        adminUserRepository.saveAndFlush(user);

        entityManager
            .createNativeQuery(
                "update admin_users set created_at = :createdAt where username = :username"
            )
            .setParameter("createdAt", Instant.now().minus(age))
            .setParameter("username", username)
            .executeUpdate();
        // Drop the cached entity so later reads see the back-dated row.
        entityManager.clear();
    }
}
