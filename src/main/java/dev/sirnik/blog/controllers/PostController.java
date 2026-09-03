package dev.sirnik.blog.controllers;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.repositories.BlogPostRepository;

@Controller
@RequestMapping("/posts")
public class PostController {

    private static final DateTimeFormatter POST_DATE_FORMATTER = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneOffset.UTC);

    private final BlogPostRepository postRepository;

    public PostController(BlogPostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping("/{slug}")
    public String individualPost(@PathVariable String slug, Locale locale,
            Model model) {
        Optional<BlogPost> post = postRepository.findBySlug(slug);

        if (post.isEmpty()) {
            return "error/404";
        }

        model.addAttribute("post", post.get());
        model.addAttribute("postDateFormatter",
                POST_DATE_FORMATTER.withLocale(locale));

        return "post";
    }
}
