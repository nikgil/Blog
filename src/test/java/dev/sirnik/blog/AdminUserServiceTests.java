package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Autowired
    AdminUserServiceTests(AdminUserRepository adminUserRepository,
        AdminUserService adminUserService) {
        this.adminUserRepository = adminUserRepository;
        this.adminUserService = adminUserService;
    }

    @Test
    void savedAdminUserLoadsWithHashAndAdminRole() {
        adminUserRepository
            .saveAndFlush(new AdminUser("owner", "{bcrypt}hash"));

        UserDetails details = adminUserService.loadUserByUsername("owner");

        assertThat(details.getUsername()).isEqualTo("owner");
        assertThat(details.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    void newAdminUserGetsCreatedAtAndNoLastLogin() {
        AdminUser saved = adminUserRepository
            .saveAndFlush(new AdminUser("owner", "{bcrypt}hash"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getLastLoginAt()).isNull();
    }

    @Test
    void unknownUsernameThrowsUsernameNotFound() {
        assertThatThrownBy(() -> adminUserService.loadUserByUsername("nobody"))
            .isInstanceOf(UsernameNotFoundException.class);
    }
}
