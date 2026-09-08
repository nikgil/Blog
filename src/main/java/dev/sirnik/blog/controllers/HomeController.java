package dev.sirnik.blog.controllers;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import dev.sirnik.blog.models.projections.ArchiveMonth;
import dev.sirnik.blog.models.views.BlogPostPreviewView;
import dev.sirnik.blog.services.BlogPostPreviewService;

@Controller
public class HomeController {

    private static final DateTimeFormatter POST_DATE_FORMATTER = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneOffset.UTC);
    private static final int PAGE_SIZE = 10;
    private final BlogPostPreviewService blogPostPreviewService;

    public HomeController(BlogPostPreviewService blogPostPreviewService) {
        this.blogPostPreviewService = blogPostPreviewService;
    }

    @GetMapping("/")
    public String home(Model model) {
        return "index";
    }

    @GetMapping("/test-home")
    public String testHome(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "throttle", defaultValue = "false") boolean throttle,
        @RequestParam(name = "year", required = false) Integer year,
        @RequestParam(name = "month", required = false) Integer month,
        @RequestParam(name = "tag", required = false) String tagSlug,
        Locale locale, Model model) {
        page = Math.max(0, page);

        if (page > 0 && throttle) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // Doesn't matter
            }
        }

        if (month != null && year == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Month requires a year");
        }

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        TimeFilter timeFilter = createTimeFilter(year, month);

        Slice<BlogPostPreviewView> posts = blogPostPreviewService
            .findPreviews(pageable, timeFilter, tagSlug);
        Map<Integer, List<ArchiveMonth>> sortedMonths = blogPostPreviewService
            .getArchiveMonths()
            .stream()
            .collect(Collectors
                .groupingBy(ArchiveMonth::getYear, LinkedHashMap::new,
                    Collectors.toList()));

        model.addAttribute("archiveMonths", sortedMonths);
        model.addAttribute("posts", posts.getContent());
        model.addAttribute("hasNext", posts.hasNext());
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("throttle", throttle);
        model.addAttribute("selectedTagSlug", tagSlug);
        model
            .addAttribute("postDateFormatter",
                POST_DATE_FORMATTER.withLocale(locale));
        return "index";
    }

    private TimeFilter createTimeFilter(Integer year, Integer month) {
        if (year == null && month == null) {
            return null;
        }

        ZonedDateTime startDateTime = YearMonth
            .of(year, month == null ? 1 : month)
            .atDay(1)
            .atStartOfDay(ZoneOffset.UTC);

        ZonedDateTime endDateTime = month == null
            ? startDateTime.plusYears(1)
            : startDateTime.plusMonths(1);

        return new TimeFilter(startDateTime.toInstant(),
            endDateTime.toInstant());
    }

    public static record TimeFilter(Instant startTime, Instant endTime) {
    }
}
