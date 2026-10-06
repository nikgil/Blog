package dev.sirnik.blog.controllers;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.projections.BlogPostLink;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.utils.AuthenticationUtils;
import jakarta.servlet.http.HttpServletResponse;

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
        Model model, HttpServletResponse response,
        Authentication authentication) {
        Optional<BlogPost> post = postRepository
            .findBySlugAndPublishedTrue(slug);
        if (post.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "error/404";
        }

        BlogPost postObj = post.get();

        BlogPostLink olderPost = postRepository
            .findOlderPublished(postObj.getCreatedAt(), postObj.getId());

        BlogPostLink newerPost = postRepository
            .findNewerPublished(postObj.getCreatedAt(), postObj.getId());

        model.addAttribute("post", postObj);
        model.addAttribute("olderPost", olderPost);
        model.addAttribute("newerPost", newerPost);
        model
            .addAttribute("postDateFormatter",
                POST_DATE_FORMATTER.withLocale(locale));

        if (AuthenticationUtils.isValidAdmin(authentication)) {
            model.addAttribute("loggedIn", true);
        } else {
            // Force admins to see freshest data
            response
                .setHeader(HttpHeaders.CACHE_CONTROL,
                    CacheControl
                        .maxAge(Duration.ofSeconds(60))
                        .cachePrivate()
                        .getHeaderValue());
        }

        return "post";
    }

    @PostMapping("/{slug}/publish")
    public String togglePublish(
        @PathVariable String slug,
        Authentication authentication,
        HttpServletResponse response,
        Model model) {
        // TODO: add proper errors for loading
        if (!AuthenticationUtils.isValidAdmin(authentication)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            Optional<BlogPost> post = postRepository
                .findBySlug(slug);

            if (post.isPresent()) {
                BlogPost postObj = post.get();
                postObj.setPublished(!postObj.isPublished());
                model.addAttribute("post", postObj);

                postRepository.save(postObj);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        return "partials/publish-toggle";
    }
}
