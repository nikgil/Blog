package dev.sirnik.blog.models.forms;

import java.util.Locale;

/**
 * Backing object for the register form. The template reads it through
 * {@code <@spring.bind "registrationForm.email" />}, so the model must hold one
 * under the name {@code registrationForm} whenever the form renders, including
 * the first GET. Spring MVC fills it from the POSTed fields through the
 * setters.
 */
public class RegistrationForm {

    private String name = "";
    private String email = "";
    private String password = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null
            : email.trim().toLowerCase(Locale.ROOT);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
