package dev.sirnik.blog.models.projections;

public interface ArchiveMonth {
    int getYear();

    int getMonth();

    long getPostCount();
}
