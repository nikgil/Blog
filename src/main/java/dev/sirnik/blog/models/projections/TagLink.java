package dev.sirnik.blog.models.projections;

public interface TagLink {

    String getSlug();

    String getName();

    long getPostCount();
}
