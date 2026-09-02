package dev.sirnik.blog.models.projections;

import java.time.Instant;
import java.util.List;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;

// Used for the test-home setup
public class TestBlogPostPreview implements BlogPostPreview {

    private BlogPost post;

    public TestBlogPostPreview(BlogPost post) {
        this.post = post;
    }

    @Override
    public String getPreview() {
        return post.getPreview();
    }

    @Override
    public Instant getCreatedAt() {
        return post.getCreatedAt();
    }

    @Override
    public String getSlug() {
        return post.getSlug();
    }

    @Override
    public String getTitle() {
        return post.getTitle();
    }

    @Override
    public List<Tag> getOrderedTags() {
        return post.getOrderedTags();
    }

}
