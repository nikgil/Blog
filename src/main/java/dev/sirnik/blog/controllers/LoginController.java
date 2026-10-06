package dev.sirnik.blog.controllers;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.sirnik.blog.repositories.AdminUserRepository;

@Controller
public class LoginController {

    private static final DateTimeFormatter LOGIN_TIME = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC);

    private final AdminUserRepository adminUserRepository;

    public LoginController(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @GetMapping("/login")
    public String getLogin(
        @RequestParam(defaultValue = "false") boolean error,
        @RequestParam(defaultValue = "false") boolean logout,
        @RequestParam(defaultValue = "false") boolean success,
        Model model,
        Authentication authentication
    ) {

        model.addAttribute("error", error);
        model.addAttribute("logout", logout);
        model.addAttribute("success", success);

        if (authentication != null) {
            adminUserRepository
                .findPrevLoginAtByUsername(authentication.getName())
                .map(LOGIN_TIME::format)
                .ifPresent(time -> model.addAttribute("previousLogin", time));
        }

        return "login";
    }
}
