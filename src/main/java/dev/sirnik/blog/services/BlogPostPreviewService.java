package dev.sirnik.blog.services;

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

import dev.sirnik.blog.controllers.HomeController.TimeFilter;
import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.projections.ArchiveMonth;
import dev.sirnik.blog.models.projections.BlogPostPreview;
import dev.sirnik.blog.models.projections.BlogPostTagRow;
import dev.sirnik.blog.models.views.BlogPostPreviewView;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.BlogPostPredicates;
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
        TimeFilter timeFilter, String tagSlug) {
        PredicateSpecification<BlogPost> filters = BlogPostPredicates
            .isPublished()
            .and(BlogPostPredicates.hasTagSlug(tagSlug));

        if (timeFilter != null) {
            filters = filters
                .and(
                    BlogPostPredicates.createdAtOrAfter(timeFilter.startTime()))
                .and(BlogPostPredicates.createdBefore(timeFilter.endTime()));
        }

        Pageable previewPageable = PageRequest
            .of(pageable.getPageNumber(), pageable.getPageSize(), PREVIEW_SORT);
        Slice<BlogPostPreview> postSlice = blogPostRepository
            .findBy(filters,
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
}
