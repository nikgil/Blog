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

import dev.sirnik.blog.models.forms.RegistrationForm;

// Only the boolean and int keys need a value here: an unresolved ${...}
// placeholder fails binding for those types, but for Strings it just binds as
// literal text. Setting them keeps this test independent of a local .env.
@SpringBootTest(properties = {"blog.registration.enabled=true",
    "blog.registration.mail.port=587"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationPageTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerPageRendersTheFormWhenRegistrationIsEnabled()
        throws Exception {
        MvcResult result = mockMvc
            .perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attribute("registrationEnabled", true))
            .andExpect(model().attributeExists("registrationForm"))
            .andReturn();

        Document page = Jsoup.parse(result.getResponse().getContentAsString());

        // register.ftl submits through htmx (hx-post), not a plain action.
        assertThat(page.select("form[hx-post=/register]")).hasSize(1);
        assertThat(page.select("input#register-name[name=name][type=text]"))
            .hasSize(1);
        assertThat(page.select("input#register-email[name=email][type=email]"))
            .hasSize(1);
        assertThat(
            page.select("input#register-password[name=password][type=password]")
        ).hasSize(1);
        assertThat(page.select("form input[name=_csrf]")).hasSize(1);
        assertThat(page.select(".is-danger")).isEmpty();
    }

    @Test
    void registerPageStartsWithAnEmptyForm() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/register"))
            .andExpect(status().isOk())
            .andReturn();

        RegistrationForm form = (RegistrationForm) result
            .getModelAndView()
            .getModel()
            .get("registrationForm");

        assertThat(form.getName()).isEmpty();
        assertThat(form.getEmail()).isEmpty();
        assertThat(form.getPassword()).isEmpty();
    }
}
