package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.utils.JsonWebTokenUtils;
import dev.sirnik.blog.utils.JsonWebTokenUtils.JWTPayload;

/**
 * The owner opens the emailed approval link: {@code /register/confirm} must
 * activate the pending user and render {@code verify_success}. The token is
 * minted with the same secret the app reads from {@code blog.registration.jwt},
 * and {@code @Transactional} rolls the pending user back after each test.
 */
@SpringBootTest(properties = {"blog.registration.enabled=true",
    "blog.registration.mail.port=587",
    "blog.registration.jwt=0123456789abcdef0123456789abcdef"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationConfirmPageTests {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    void approvalLinkActivatesTheUserAndShowsTheSuccessPage() throws Exception {
        AdminUser pending = savePendingUser("ada", "ada@example.com");
        String token = tokenFor(pending);

        MvcResult result = mockMvc
            .perform(
                get("/register/confirm")
                    .param("confirmation", token)
                    .with(user("owner").roles("ADMIN"))
            )
            .andExpect(status().isOk())
            .andExpect(view().name("verify_success"))
            .andExpect(model().attribute("username", "ada"))
            .andExpect(model().attribute("email", "ada@example.com"))
            .andReturn();

        Document page = Jsoup.parse(result.getResponse().getContentAsString());

        assertThat(page.select("main h2").text()).isEqualTo("Account approved");
        assertThat(page.select("main strong").text()).isEqualTo("ada");
        assertThat(page.select("main .content").text())
            .contains("ada@example.com");
        assertThat(page.select("main a.button[href=/]")).hasSize(1);
        assertThat(
            adminUserRepository
                .findByUsername("ada")
                .orElseThrow()
                .isActivated()
        ).isTrue();
    }

    @Test
    void successPageEscapesTheRegistrantsName() throws Exception {
        // Names are free text from the public register form, and the owner is
        // the one who sees this page.
        String nasty = "<script>alert(1)</script>";
        AdminUser pending = savePendingUser(nasty, "nasty@example.com");

        MvcResult result = mockMvc
            .perform(
                get("/register/confirm")
                    .param("confirmation", tokenFor(pending))
                    .with(user("owner").roles("ADMIN"))
            )
            .andExpect(status().isOk())
            .andReturn();

        Document page = Jsoup.parse(result.getResponse().getContentAsString());

        assertThat(page.select("main script")).isEmpty();
        assertThat(page.select("main strong").text()).isEqualTo(nasty);
    }

    @Test
    void visitorCannotUseAnApprovalLink() throws Exception {
        AdminUser pending = savePendingUser("ada", "ada@example.com");

        mockMvc
            .perform(
                get("/register/confirm")
                    .param("confirmation", tokenFor(pending))
            )
            .andExpect(status().isUnauthorized());

        assertThat(
            adminUserRepository
                .findByUsername("ada")
                .orElseThrow()
                .isActivated()
        ).isFalse();
    }

    private AdminUser savePendingUser(
        String name,
        String email
    ) {
        return adminUserRepository
            .saveAndFlush(
                new AdminUser(
                    email,
                    name,
                    "{noop}unused-hash"
                )
            );
    }

    private static String tokenFor(AdminUser user) throws Exception {
        return JsonWebTokenUtils
            .encodePayload(
                JWT_SECRET, new JWTPayload(
                    user.getUsername(),
                    user.getEmail(),
                    user.getCreatedAt().toEpochMilli()
                )
            );
    }
}
