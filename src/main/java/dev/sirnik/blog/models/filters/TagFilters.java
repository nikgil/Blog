package dev.sirnik.blog.models.filters;

public class TagFilters {

    private String tagQuery = "";
    private Integer tagPage = 0;

    public String getTagQuery() {
        return tagQuery;
    }

    public void setTagQuery(String tagQuery) {
        this.tagQuery = tagQuery;
    }

    public Integer getTagPage() {
        return tagPage;
    }

    public void setTagPage(Integer tagPage) {
        this.tagPage = tagPage;
    }
}
