package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.services.AdminUserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserServiceTests {

    private final AdminUserRepository adminUserRepository;
    private final AdminUserService adminUserService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    AdminUserServiceTests(
        AdminUserRepository adminUserRepository,
        AdminUserService adminUserService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.adminUserRepository = adminUserRepository;
        this.adminUserService = adminUserService;
        this.eventPublisher = eventPublisher;
    }

    @Test
    void savedAdminUserLoadsWithHashAndAdminRole() {
        adminUserRepository
            .saveAndFlush(
                new AdminUser(
                    "owner",
                    "{bcrypt}hash"
                )
            );

        UserDetails details = adminUserService.loadUserByUsername("owner");

        assertThat(details.getUsername()).isEqualTo("owner");
        assertThat(details.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    void newAdminUserGetsCreatedAtAndNoLoginTimes() {
        AdminUser saved = adminUserRepository
            .saveAndFlush(
                new AdminUser(
                    "owner",
                    "{bcrypt}hash"
                )
            );

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCurrentLoginAt()).isNull();
        assertThat(saved.getPrevLoginAt()).isNull();
    }

    @Test
    void successfulLoginEventRecordsTheLoginTime() {
        adminUserRepository
            .saveAndFlush(
                new AdminUser(
                    "owner",
                    "{bcrypt}hash"
                )
            );

        publishLoginSuccess("owner");

        AdminUser admin = adminUserRepository
            .findByUsername("owner")
            .orElseThrow();
        assertThat(admin.getCurrentLoginAt()).isNotNull();
        assertThat(admin.getPrevLoginAt()).isNull();
    }

    @Test
    void secondLoginMovesTheFirstOneIntoPreviousLogin() {
        adminUserRepository
            .saveAndFlush(
                new AdminUser(
                    "owner",
                    "{bcrypt}hash"
                )
            );

        publishLoginSuccess("owner");
        java.time.Instant firstLogin = adminUserRepository
            .findByUsername("owner")
            .orElseThrow()
            .getCurrentLoginAt();
        publishLoginSuccess("owner");

        AdminUser admin = adminUserRepository
            .findByUsername("owner")
            .orElseThrow();
        assertThat(admin.getPrevLoginAt()).isEqualTo(firstLogin);
        assertThat(admin.getCurrentLoginAt()).isAfterOrEqualTo(firstLogin);
    }

    @Test
    void loginEventForUnknownUserIsIgnored() {
        publishLoginSuccess("nobody");

        assertThat(adminUserRepository.count()).isZero();
    }

    private void publishLoginSuccess(String username) {
        eventPublisher
            .publishEvent(
                new AuthenticationSuccessEvent(
                    UsernamePasswordAuthenticationToken
                        .authenticated(username, null, java.util.List.of())
                )
            );
    }

    @Test
    void unknownUsernameThrowsUsernameNotFound() {
        assertThatThrownBy(() -> adminUserService.loadUserByUsername("nobody"))
            .isInstanceOf(UsernameNotFoundException.class);
    }
}
