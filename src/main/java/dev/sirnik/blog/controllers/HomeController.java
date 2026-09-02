package dev.sirnik.blog.controllers;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.sirnik.blog.models.projections.BlogPostPreview;
import dev.sirnik.blog.repositories.BlogPostRepository;

@Controller
public class HomeController {

    private static final DateTimeFormatter POST_DATE_FORMATTER = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneOffset.UTC);
    private static final int PAGE_SIZE = 10;
    private final BlogPostRepository blogPostRepository;

    public HomeController(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

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

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<BlogPostPreview> posts = blogPostRepository
                .findAllByOrderByCreatedAtDescIdDesc(pageable);

        model.addAttribute("posts", posts.getContent());
        model.addAttribute("hasNext", posts.hasNext());
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("throttle", throttle);
        model.addAttribute("postDateFormatter",
                POST_DATE_FORMATTER.withLocale(locale));
        return "index";
    }
}
