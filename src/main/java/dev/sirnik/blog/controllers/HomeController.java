package dev.sirnik.blog.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.utils.TestModelGenerator;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        return "index";
    }

    @GetMapping("/test-home")
    public String testHome(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "throttle", defaultValue = "false") boolean throttle,
            Model model) {
        if (page > 0 && throttle) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // Doesn't matter
            }
        }
        List<BlogPost> samplePosts = TestModelGenerator.generatePosts(page);

        model.addAttribute("posts", samplePosts);
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("throttle", throttle);
        return "index";
    }
}
