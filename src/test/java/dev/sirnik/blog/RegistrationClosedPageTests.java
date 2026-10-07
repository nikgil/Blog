package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
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

// Same reasoning as RegistrationPageTests: only the boolean and int keys need
// values. A separate class because the flag differs, which means a separate
// application context.
@SpringBootTest(properties = {"blog.registration.enabled=false",
    "blog.registration.mail.port=587"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationClosedPageTests {

    @Autowired
    private MockMvc mockMvc;

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
        assertThat(page.select("form[action=/register]")).isEmpty();
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

        assertThat(page.select("form[action=/register]")).isEmpty();
    }
}
