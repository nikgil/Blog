package dev.sirnik.blog.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import dev.sirnik.blog.models.forms.RegistrationForm;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.utils.RegistrationConfig;

@Controller
public class RegistrationController {

    private RegistrationConfig registrationConfig;
    private AdminUserRepository adminRepo;

    public RegistrationController(
        RegistrationConfig config,
        AdminUserRepository adminRepo
    ) {
        this.registrationConfig = config;
        this.adminRepo = adminRepo;
    }

    @GetMapping("/register")
    public String getRegisterPage(Model model) {
        model.addAttribute("registrationEnabled", registrationConfig.enabled());
        // register.ftl binds its fields to this object; without it the form
        // branch throws while rendering.
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String tryRegister(
        @ModelAttribute("registrationForm") RegistrationForm form,
        BindingResult result,
        Model model
    ) {
        if (!registrationConfig.enabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // TODO: make this atomic
        if (adminRepo
            .findByUsernameOrEmail(form.getName(), form.getEmail())
            .isPresent()) {
            result
                .reject(
                    "registration.duplicate",
                    "name or email is already being used"
                );
            model.addAttribute("registrationEnabled", true);
        } else {
            model.addAttribute("registered", true);
        }

        return "register";
    }
}
