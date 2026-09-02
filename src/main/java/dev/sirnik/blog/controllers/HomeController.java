package dev.sirnik.blog.controllers;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.sirnik.blog.models.projections.TestBlogPostPreview;
import dev.sirnik.blog.utils.TestModelGenerator;

@Controller
public class HomeController {

    private static final DateTimeFormatter POST_DATE_FORMATTER = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneOffset.UTC);

    @GetMapping("/")
    public String home(Model model) {
        return "index";
    }

    @GetMapping("/test-home")
    public String testHome(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "throttle", defaultValue = "false") boolean throttle,
            Locale locale,
            Model model) {
        if (page > 0 && throttle) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // Doesn't matter
            }
        }
        List<TestBlogPostPreview> samplePosts = TestModelGenerator
                .generatePosts(page).stream()
                .map(TestBlogPostPreview::new).toList();

        model.addAttribute("posts", samplePosts);
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("throttle", throttle);
        model.addAttribute("postDateFormatter",
                POST_DATE_FORMATTER.withLocale(locale));
        return "index";
    }
}
