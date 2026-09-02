package dev.sirnik.blog.models.views;

public class TagView {

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
