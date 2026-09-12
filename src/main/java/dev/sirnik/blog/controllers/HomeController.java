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
public class HomeController {

    private final BlogPageModel blogPageModel;

    public HomeController(BlogPageModel blogPageModel) {
        this.blogPageModel = blogPageModel;
    }

    // The more specific header mapping in PostListController handles fragments.
    @GetMapping("/")
    public String home(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @ModelAttribute("filters") PostFilters filters, Locale locale,
        @ModelAttribute("tagFilters") TagFilters tagFilters, Model model,
        HttpServletResponse response) {
        response
            .addHeader(HttpHeaders.VARY,
                "HX-Request, HX-History-Restore-Request");
        blogPageModel.addPosts(page, filters, locale, model);
        blogPageModel.addSidebar(tagFilters, model);
        return "index";
    }
}
