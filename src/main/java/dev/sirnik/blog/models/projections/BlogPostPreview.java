package dev.sirnik.blog.models.projections;

import java.time.Instant;

public interface BlogPostPreview {

    Long getId();

    String getPreview();

    Instant getCreatedAt();

    String getSlug();

    String getTitle();
}
