package dev.sirnik.blog.controllers;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import dev.sirnik.blog.models.filters.PostFilters;
import dev.sirnik.blog.models.filters.TagFilters;
import dev.sirnik.blog.models.projections.ArchiveMonth;
import dev.sirnik.blog.models.projections.TagLink;
import dev.sirnik.blog.models.views.BlogPostPreviewView;
import dev.sirnik.blog.repositories.TagRepository;
import dev.sirnik.blog.services.BlogPostPreviewService;

// MVC model preparation shared by the full-page and fragment controllers.
@Component
public class BlogPageModel {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter POST_DATE_FORMATTER = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneOffset.UTC);

    private final BlogPostPreviewService blogPostPreviewService;
    private final TagRepository tagRepository;

    public BlogPageModel(BlogPostPreviewService blogPostPreviewService,
        TagRepository tagRepository) {
        this.blogPostPreviewService = blogPostPreviewService;
        this.tagRepository = tagRepository;
    }

    public void addPosts(int page, PostFilters filters, Locale locale,
        Model model) {
        if (filters.getMonth() != null && filters.getYear() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Month requires a year");
        }
        page = Math.max(0, page);
        Slice<BlogPostPreviewView> posts = blogPostPreviewService
            .findPreviews(PageRequest.of(page, PAGE_SIZE), filters);
        model.addAttribute("posts", posts.getContent());
        model.addAttribute("hasNext", posts.hasNext());
        model.addAttribute("nextPage", page + 1);
        model
            .addAttribute("postDateFormatter",
                POST_DATE_FORMATTER.withLocale(locale));
    }

    public void addSidebar(TagFilters tagFilters, Model model) {
        Map<Integer, List<ArchiveMonth>> archiveMonths = blogPostPreviewService
            .getArchiveMonths()
            .stream()
            .collect(Collectors
                .groupingBy(ArchiveMonth::getYear, LinkedHashMap::new,
                    Collectors.toList()));
        model.addAttribute("archiveMonths", archiveMonths);

        int tagPage = Math
            .max(0,
                tagFilters.getTagPage() == null ? 0 : tagFilters.getTagPage());
        tagFilters.setTagPage(tagPage);
        Slice<TagLink> tags = tagRepository
            .findTagLinks(tagFilters.getTagQuery(),
                PageRequest.of(tagPage, PAGE_SIZE));
        model.addAttribute("tagFilters", tagFilters);
        model.addAttribute("tags", tags.getContent());
        model.addAttribute("hasNextTagPage", tags.hasNext());
        model.addAttribute("hasPreviousTagPage", tags.hasPrevious());
    }
}
