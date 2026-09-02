package dev.sirnik.blog.models.projections;

import java.time.Instant;
import java.util.List;

import dev.sirnik.blog.models.Tag;

public interface BlogPostPreview {

    String getPreview();

    Instant getCreatedAt();

    String getSlug();

    String getTitle();

    List<Tag> getOrderedTags();
}
