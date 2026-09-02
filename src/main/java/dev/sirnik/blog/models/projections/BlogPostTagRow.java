package dev.sirnik.blog.models.projections;

public interface BlogPostTagRow {

    Long getPostId();

    String getName();

    String getSlug();
}
