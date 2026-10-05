package dev.sirnik.blog.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String getLogin(@RequestParam(defaultValue = "false") boolean error,
        @RequestParam(defaultValue = "false") boolean logout, Model model) {
        model.addAttribute("error", error);
        model.addAttribute("logout", logout);
        return "login";
    }
}
