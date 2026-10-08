package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;

/**
 * The "dev" profile is neither prod nor test, so DevelopmentDataConfiguration
 * runs and seeds the local login. The other test classes use the "test"
 * profile, which skips the seeding on purpose.
 */
@SpringBootTest(properties = {"blog.registration.enabled=false",
    "blog.registration.mail.port=587"})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevelopmentSeedUserTests {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devProfileSeedsExactlyOneActivatedUser() {
        assertThat(adminUserRepository.count()).isEqualTo(1);

        AdminUser seeded = adminUserRepository
            .findByUsername("user")
            .orElseThrow();

        assertThat(seeded.getEmail()).isEqualTo("admin@sirnik.dev");
        assertThat(seeded.isActivated()).isTrue();
        assertThat(seeded.getPasswordHash()).isNotEqualTo("password");
        assertThat(
            passwordEncoder.matches("password", seeded.getPasswordHash())
        ).isTrue();
    }

    @Test
    void seededUserCanLogInByUsername() throws Exception {
        mockMvc
            .perform(formLogin("/login").user("user").password("password"))
            .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void seededUserCanLogInByEmail() throws Exception {
        mockMvc
            .perform(
                formLogin("/login")
                    .user("admin@sirnik.dev")
                    .password("password")
            )
            .andExpect(authenticated().withRoles("ADMIN"));
    }
}
