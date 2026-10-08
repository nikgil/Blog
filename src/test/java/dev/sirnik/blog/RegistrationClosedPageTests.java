package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import dev.sirnik.blog.repositories.AdminUserRepository;

// A separate class from RegistrationPageTests because the flag differs, which
// means a separate application context. The other blog.registration.* and mail
// settings come from application-test.properties.
@SpringBootTest(properties = "blog.registration.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationClosedPageTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void registerPageShowsAClosedNoticeWhenRegistrationIsDisabled()
        throws Exception {
        MvcResult result = mockMvc
            .perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attribute("registrationEnabled", false))
            .andReturn();

        Document page = Jsoup.parse(result.getResponse().getContentAsString());

        assertThat(page.select("h2").text())
            .isEqualTo("Registration is closed");
        assertThat(page.select("form[hx-post=/register]")).isEmpty();
    }

    @Test
    void aQueryParameterCannotReopenRegistration() throws Exception {
        // Regression: the config was once a handler parameter, which Spring MVC
        // binds from the request instead of injecting the configured bean.
        MvcResult result = mockMvc
            .perform(get("/register").param("enabled", "true"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("registrationEnabled", false))
            .andReturn();

        Document page = Jsoup.parse(result.getResponse().getContentAsString());

        assertThat(page.select("form[hx-post=/register]")).isEmpty();
    }

    @Test
    void postingToRegisterIsRefusedAndNothingIsSavedOrEmailed()
        throws Exception {
        // The page hides the form, but the POST endpoint must refuse on its
        // own: a script does not need the page.
        mockMvc
            .perform(
                post("/register")
                    .with(csrf())
                    .param("name", "ada")
                    .param("email", "ada@example.com")
                    .param("password", "password")
            )
            .andExpect(status().isUnauthorized());

        assertThat(adminUserRepository.findByUsername("ada")).isEmpty();
        verifyNoInteractions(mailSender);
    }
}
