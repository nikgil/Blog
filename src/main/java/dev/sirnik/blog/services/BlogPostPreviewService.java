package dev.sirnik.blog.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.projections.BlogPostPreview;
import dev.sirnik.blog.models.projections.BlogPostTagRow;
import dev.sirnik.blog.models.views.BlogPostPreviewView;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;

@Service
public class BlogPostPreviewService {

    private final BlogPostRepository blogPostRepository;
    private final TagRepository tagRepository;

    public BlogPostPreviewService(
            BlogPostRepository blogPostRepository,
            TagRepository tagRepository) {
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public Slice<BlogPostPreviewView> findPreviews(Pageable pageable) {
        Slice<BlogPostPreview> postSlice = blogPostRepository
                .findAllByOrderByCreatedAtDescIdDesc(pageable);

        List<Long> postIds = postSlice.getContent().stream()
                .map(BlogPostPreview::getId)
                .toList();
        Map<Long, List<BlogPostTagRow>> tagsByPostId = loadTags(postIds);

        return postSlice.map(post -> new BlogPostPreviewView(
                post,
                tagsByPostId.getOrDefault(post.getId(),
                        Collections.emptyList())));
    }

    private Map<Long, List<BlogPostTagRow>> loadTags(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return tagRepository.findForPostIds(postIds).stream()
                .collect(Collectors.groupingBy(
                        BlogPostTagRow::getPostId,
                        Collectors.toList()));
    }
}
