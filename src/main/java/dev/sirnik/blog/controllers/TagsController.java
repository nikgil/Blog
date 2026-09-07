package dev.sirnik.blog.controllers;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.repositories.TagRepository;

@Controller
public class TagsController {

    private static final int PAGE_SIZE = 10;
    private final TagRepository tagRepo;

    public TagsController(TagRepository tagRepository) {
        this.tagRepo = tagRepository;
    }

    @GetMapping("/tags")
    public String getTags(
        @RequestParam(name = "page", defaultValue = "0") int page,
        Model model) {
        page = Math.max(0, page);

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<Tag> tags = tagRepo.findAllByOrderByName(pageable);

        model.addAttribute("tags", tags.getContent());
        model.addAttribute("hasNextTagPage", tags.hasNext());
        model.addAttribute("hasPreviousTagPage", tags.hasPrevious());
        model.addAttribute("tagPage", page);
        return "partials/tag-list";
    }
}
