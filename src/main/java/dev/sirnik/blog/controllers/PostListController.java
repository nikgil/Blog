package dev.sirnik.blog.controllers;

import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import dev.sirnik.blog.models.filters.PostFilters;
import dev.sirnik.blog.models.filters.TagFilters;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PostListController {

    private final BlogPageModel blogPageModel;

    public PostListController(BlogPageModel blogPageModel) {
        this.blogPageModel = blogPageModel;
    }

    // History cache misses need the full page from HomeController.
    @GetMapping(value = "/", headers = {"HX-Request=true",
        "HX-History-Restore-Request!=true"})
    public String posts(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @ModelAttribute("filters") PostFilters filters, Locale locale,
        @ModelAttribute("tagFilters") TagFilters tagFilters, Model model,
        HttpServletResponse response) {
        response
            .addHeader(HttpHeaders.VARY,
                "HX-Request, HX-History-Restore-Request");
        blogPageModel.addPosts(page, filters, locale, model);

        if (page > 0) {
            return "partials/post-items";
        }
        blogPageModel.addSidebar(tagFilters, model);
        return "partials/blog-content";
    }
}
