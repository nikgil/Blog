package dev.sirnik.blog.models.views;

import java.time.Instant;
import java.util.List;

import dev.sirnik.blog.models.projections.BlogPostPreview;
import dev.sirnik.blog.models.projections.BlogPostTagRow;

public class BlogPostPreviewView {

    private final Long id;
    private final String preview;
    private final Instant createdAt;
    private final String slug;
    private final String title;
    private final List<TagView> orderedTags;

    public BlogPostPreviewView(
            BlogPostPreview post,
            List<BlogPostTagRow> orderedTagRows) {
        id = post.getId();
        preview = post.getPreview();
        createdAt = post.getCreatedAt();
        slug = post.getSlug();
        title = post.getTitle();

        // Keep persistence projections out of the template-facing model.
        orderedTags = orderedTagRows.stream()
                .map(row -> new TagView(row.getName(), row.getSlug()))
                .toList();
    }

    public Long getId() {
        return id;
    }

    public String getPreview() {
        return preview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public List<TagView> getOrderedTags() {
        return orderedTags;
    }

    public static class TagView {

        private final String name;
        private final String slug;

        public TagView(String name, String slug) {
            this.name = name;
            this.slug = slug;
        }

        public String getName() {
            return name;
        }

        public String getSlug() {
            return slug;
        }
    }
}
