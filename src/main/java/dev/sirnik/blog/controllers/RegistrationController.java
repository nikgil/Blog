package dev.sirnik.blog.controllers;

import java.text.ParseException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.nimbusds.jose.JOSEException;

import dev.sirnik.blog.models.forms.RegistrationForm;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.services.UserRegistrationService;
import dev.sirnik.blog.utils.AuthenticationUtils;
import dev.sirnik.blog.utils.JsonWebTokenUtils;
import dev.sirnik.blog.utils.JsonWebTokenUtils.JWTPayload;
import dev.sirnik.blog.utils.RegistrationConfig;

@Controller
public class RegistrationController {

    private RegistrationConfig registrationConfig;
    private UserRegistrationService regService;
    private AdminUserRepository adminUserRepository;

    public RegistrationController(
        RegistrationConfig config,
        UserRegistrationService regService,
        AdminUserRepository adminUserRepository
    ) {
        this.registrationConfig = config;
        this.regService = regService;
        this.adminUserRepository = adminUserRepository;
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
        if (!regService.isValidRegistrationForm(form)) {
            result
                .reject(
                    "registration.duplicate",
                    "name or email is already being used"
                );
            model.addAttribute("registrationEnabled", true);
        } else {
            regService.saveUser(form);
            model.addAttribute("registered", true);
        }

        return "register";
    }

    @GetMapping("/register/confirm")
    public String getConfirmation(
        @RequestParam(name = "confirmation") String confirmation,
        Authentication auth,
        Model model
    ) {
        if (!AuthenticationUtils.isValidAdmin(auth)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            // TODO: move to user reg service
            JWTPayload payload = JsonWebTokenUtils
                .parsePayload(registrationConfig.jwt(), confirmation);

            boolean isValid = JsonWebTokenUtils
                .verifyPayload(
                    payload, adminUserRepository, System.currentTimeMillis()
                );

            if (!isValid) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            regService.activateUser(payload.username());
            model.addAttribute("username", payload.username());
            model.addAttribute("email", payload.email());
            return "verify_success";
        } catch (JOSEException | ParseException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
