package dev.sirnik.blog.services;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import dev.sirnik.blog.utils.AuthenticationUtils;

@ControllerAdvice
public class AuthenticationModelAdvice {

    @ModelAttribute("loggedIn")
    public boolean isLoggedIn(Authentication authentication) {
        return AuthenticationUtils.isValidAdmin(authentication);
    }
}
