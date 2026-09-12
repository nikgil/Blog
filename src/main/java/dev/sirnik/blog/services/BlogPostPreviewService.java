package dev.sirnik.blog.services;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.filters.PostFilters;
import dev.sirnik.blog.models.projections.ArchiveMonth;
import dev.sirnik.blog.models.projections.BlogPostPreview;
import dev.sirnik.blog.models.projections.BlogPostTagRow;
import dev.sirnik.blog.models.views.BlogPostPreviewView;
import dev.sirnik.blog.repositories.BlogPostPredicates;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;

@Service
public class BlogPostPreviewService {

    private static final Sort PREVIEW_SORT = Sort
        .by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private final BlogPostRepository blogPostRepository;
    private final TagRepository tagRepository;

    public BlogPostPreviewService(BlogPostRepository blogPostRepository,
        TagRepository tagRepository) {
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
    }

    public List<ArchiveMonth> getArchiveMonths() {
        return blogPostRepository.findAllBlogPostsMonths();
    }

    @Transactional(readOnly = true)
    public Slice<BlogPostPreviewView> findPreviews(Pageable pageable,
        PostFilters filters) {
        PredicateSpecification<BlogPost> predicate = BlogPostPredicates
            .isPublished()
            .and(BlogPostPredicates.hasTagSlug(filters.getTag()))
            .and(BlogPostPredicates.contentContains(filters.getQuery()));

        TimeFilter timeFilter = createTimeFilter(filters.getYear(),
            filters.getMonth());

        if (timeFilter != null) {
            predicate = predicate
                .and(
                    BlogPostPredicates.createdAtOrAfter(timeFilter.startTime()))
                .and(BlogPostPredicates.createdBefore(timeFilter.endTime()));
        }

        Pageable previewPageable = PageRequest
            .of(pageable.getPageNumber(), pageable.getPageSize(), PREVIEW_SORT);
        Slice<BlogPostPreview> postSlice = blogPostRepository
            .findBy(predicate,
                query -> query
                    .as(BlogPostPreview.class)
                    .slice(previewPageable));

        List<Long> postIds = postSlice
            .getContent()
            .stream()
            .map(BlogPostPreview::getId)
            .toList();
        Map<Long, List<BlogPostTagRow>> tagsByPostId = loadTags(postIds);

        return postSlice
            .map(post -> new BlogPostPreviewView(post, tagsByPostId
                .getOrDefault(post.getId(), Collections.emptyList())));
    }

    private TimeFilter createTimeFilter(Integer year, Integer month) {
        if (year == null) {
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

    private Map<Long, List<BlogPostTagRow>> loadTags(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return tagRepository
            .findForPostIds(postIds)
            .stream()
            .collect(Collectors
                .groupingBy(BlogPostTagRow::getPostId, Collectors.toList()));
    }

    private record TimeFilter(Instant startTime, Instant endTime) {
    }
}
