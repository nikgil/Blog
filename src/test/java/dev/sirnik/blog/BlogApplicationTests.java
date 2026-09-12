package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.controllers.HomeController;
import dev.sirnik.blog.controllers.PostListController;
import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.models.projections.TagLink;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;
import freemarker.template.Configuration;
import jakarta.servlet.RequestDispatcher;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BlogApplicationTests {

    private final MockMvc mockMvc;
    private final BlogPostRepository blogPostRepository;
    private final TagRepository tagRepository;
    private final Configuration freeMarkerConfiguration;

    @Autowired
    BlogApplicationTests(MockMvc mockMvc, BlogPostRepository blogPostRepository,
        TagRepository tagRepository, Configuration freeMarkerConfiguration) {
        this.mockMvc = mockMvc;
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
        this.freeMarkerConfiguration = freeMarkerConfiguration;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void homePageLoads() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .handler()
                .handlerType(HomeController.class))
            .andExpect(MockMvcResultMatchers.view().name("index"))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("sirnik.Dev")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/css/index.css\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("aria-label=\"Post archive\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("id=\"archive-years\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("id=\"tag-filter-query\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("name=\"tagQuery\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("id=\"tag-filter-results\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "id=\"post-filters\" class=\"site-search\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("hx-target=\"#blog-content\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "class=\"site-search__indicator htmx-indicator\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("<main id=\"blogs\">")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"mailto:blog@sirnik.dev\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/ai-usage\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/about\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "hx-include=\"#tag-filter-query, #tag-page-state\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("posts-filter"))))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("Loading tags…"))))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("id=\"tag-page-previous\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("id=\"tag-page-next\"")));
    }

    @Test
    void searchWithNoMatchesRendersEmptyState() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .param("query", "definitely-not-in-a-post"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("class=\"search-empty\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("No results found")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Try a different search term.")));
    }

    @Test
    void tagListPartialRendersAtMostTenTags() throws Exception {
        List<TagLink> tags = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tags.add(new TestTagLink("Tag " + i, "tag-" + i, i));
        }

        StringWriter rendered = new StringWriter();
        freeMarkerConfiguration
            .getTemplate("partials/tag-list.ftl")
            .process(Map.of("tags", tags, "tagPage", 0, "hasNextTagPage", true),
                rendered);

        assertThat(rendered.toString())
            .contains("id=\"tag-list\"")
            .contains("href=\"/?tag=tag-0\"")
            .contains("<span>(0)</span>")
            .contains("Tag 9")
            .doesNotContain("Tag 10")
            .contains("name=\"page\"")
            .contains("value=\"1\"")
            .contains("aria-label=\"Tag page 1\"");
    }

    @Test
    void tagListRequestPreservesSelectionAndSearchAcrossPagination()
        throws Exception {
        tagRepository.save(new Tag("Spring", "spring"));
        for (int i = 0; i < 10; i++) {
            tagRepository.save(new Tag("Spring " + i, "spring-" + i));
        }
        tagRepository.flush();

        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/tags")
                .param("tagQuery", "Spring")
                .param("query", "backend")
                .param("tag", "spring")
                .param("year", "2025")
                .param("month", "1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("partials/tag-list"))
            .andExpect(
                MockMvcResultMatchers.model().attribute("tagQuery", "Spring"))
            .andExpect(MockMvcResultMatchers
                .model()
                .attribute("filters", hasProperty("query", is("backend"))))
            .andExpect(MockMvcResultMatchers
                .model()
                .attribute("filters", hasProperty("tag", is("spring"))))
            .andExpect(MockMvcResultMatchers
                .model()
                .attribute("filters", hasProperty("year", is(2025))))
            .andExpect(MockMvcResultMatchers
                .model()
                .attribute("filters", hasProperty("month", is(1))))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "class=\"tag-filter__item tag-selected__item\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("aria-current=\"true\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("<span>(0)</span>")))
            .andExpect(result -> {
                Document page = Jsoup
                    .parse(result.getResponse().getContentAsString());
                assertThat(page
                    .select(".tag-filter__link[aria-current=true]")
                    .eachAttr("href"))
                    .containsExactly("/?year=2025&month=1&query=backend");
                assertThat(page
                    .select(".tag-filter__link:not([aria-current])")
                    .eachAttr("href"))
                    .contains("/?tag=spring-0&year=2025&month=1&query=backend");
            })
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("type=\"submit\" name=\"page\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("value=\"1\"")));
    }

    @Test
    void notFoundPageLinksBackToHome() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI,
                    "/missing-page"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.view().name("error/404"))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("This page wandered off.")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Back to all posts")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/css/error.css\"")));
    }

    @Test
    void genericErrorPageLinksBackToHome() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI,
                    "/failed-page"))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError())
            .andExpect(MockMvcResultMatchers.view().name("error"))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Something went wrong.")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Back to all posts")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/css/error.css\"")));
    }

    @Test
    void publishedPostRendersWithTagsNavigationAndCacheHeader()
        throws Exception {
        BlogPost olderPost = savePost("Older post", "older-post", true);

        Tag java = tagRepository.save(new Tag("Java", "java"));
        BlogPost currentPost = new BlogPost("Current post", "current-post",
            "<p>Current article content.</p>");
        currentPost.addTag(java);
        currentPost.setPublished(true);
        blogPostRepository.save(currentPost);

        BlogPost newerPost = savePost("Newer post", "newer-post", true);
        blogPostRepository.flush();

        mockMvc
            .perform(MockMvcRequestBuilders.get("/posts/current-post"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("post"))
            .andExpect(MockMvcResultMatchers
                .model()
                .attribute("post", hasProperty("slug", is("current-post"))))
            .andExpect(MockMvcResultMatchers
                .model()
                .attributeExists("postDateFormatter", "olderPost", "newerPost"))
            .andExpect(MockMvcResultMatchers
                .header()
                .string(HttpHeaders.CACHE_CONTROL,
                    containsString("max-age=60")))
            .andExpect(MockMvcResultMatchers
                .header()
                .string(HttpHeaders.CACHE_CONTROL, containsString("private")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Current post")))
            .andExpect(
                MockMvcResultMatchers.content().string(containsString("Java")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/css/post.css\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "href=\"/posts/" + olderPost.getSlug() + "\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "href=\"/posts/" + newerPost.getSlug() + "\"")));
    }

    @Test
    void postWithoutAdjacentPostsOnlyRendersHomeNavigation() throws Exception {
        savePost("Only post", "only-post", true);
        blogPostRepository.flush();

        mockMvc
            .perform(MockMvcRequestBuilders.get("/posts/only-post"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("Previous post"))))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("Next post"))))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("href=\"/\"")))
            .andExpect(
                MockMvcResultMatchers.content().string(containsString("Home")));
    }

    @Test
    void missingPostReturnsNotFoundPage() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/posts/does-not-exist"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.view().name("error/404"))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("This page wandered off.")));
    }

    @Test
    void unpublishedPostReturnsNotFoundPage() throws Exception {
        savePost("Draft post", "draft-post", false);
        blogPostRepository.flush();

        mockMvc
            .perform(MockMvcRequestBuilders.get("/posts/draft-post"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.view().name("error/404"));
    }

    @Test
    void postTemplateRendersPostAndAdjacentNavigation() throws Exception {
        BlogPost post = new BlogPost("Rendered post", "rendered-post",
            "<p>Trusted article content.</p>");
        post.addTag(new Tag("Zulu", "zulu"));
        post.addTag(new Tag("Alpha", "alpha"));
        post.setCreationTimestamps();

        Map<String, Object> model = new HashMap<>();
        model.put("post", post);
        model
            .put("postDateFormatter",
                DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC));
        model
            .put("olderPost",
                Map.of("title", "Older post", "slug", "older-post"));
        model
            .put("newerPost",
                Map.of("title", "Newer post", "slug", "newer-post"));

        StringWriter rendered = new StringWriter();
        freeMarkerConfiguration
            .getTemplate("post.ftl")
            .process(model, rendered);

        assertThat(rendered.toString())
            .contains("Rendered post")
            .contains("<p>Trusted article content.</p>")
            .containsSubsequence("Alpha", "Zulu")
            .contains("href=\"/posts/older-post\"")
            .contains("href=\"/\"")
            .contains("Home")
            .contains("href=\"/posts/newer-post\"")
            .contains("preload=\"mouseover\"")
            .contains("href=\"/css/post.css\"")
            .contains("src=\"/js/post.js\"");
    }

    @Test
    void homeRendersOrderedPostPreviewsAndNextSliceTrigger() throws Exception {
        Tag zulu = tagRepository.save(new Tag("Zulu", "zulu"));
        Tag alpha = tagRepository.save(new Tag("Alpha", "alpha"));
        List<BlogPost> posts = new ArrayList<>();

        // Eleven records make the first ten-record Slice report
        // hasNext=true.
        // Reverse insertion proves display order comes from the query.
        for (int i = 0; i < 11; i++) {
            BlogPost post = new BlogPost("Post " + i, "post-" + i,
                "<p>Lorem ipsum preview " + i + ".</p>");
            post.addTag(zulu);
            post.addTag(alpha);
            post.setPublished(true);
            posts.add(post);
        }
        blogPostRepository.saveAllAndFlush(posts);

        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .param("page", "0")
                .param("tag", "alpha"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .handler()
                .handlerType(HomeController.class))
            .andExpect(MockMvcResultMatchers.view().name("index"))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("post-preview__tag-scroll")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("post-preview__body")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("class=\"post-preview__date\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("archive-nav__year-link")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("archive-nav__month-link")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("post-preview__loading htmx-indicator")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Loading more posts…")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("&amp;tag=alpha")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Lorem ipsum")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(stringContainsInOrder("Alpha", "Zulu")));

        // The last Slice has no successor, so it must not render a new
        // trigger.
        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .param("page", "1")
                .param("tag", "alpha"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Post 0")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("post-preview__loading"))));
    }

    @Test
    void homeCombinesPublishedTimeAndTagFilters() throws Exception {
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        Tag java = tagRepository.save(new Tag("Java", "java"));

        BlogPost matching = postAt("Matching post", "matching-post",
            "2025-01-15T12:00:00Z", true, spring);
        BlogPost wrongTag = postAt("Wrong tag", "wrong-tag",
            "2025-01-16T12:00:00Z", true, java);
        BlogPost wrongMonth = postAt("Wrong month", "wrong-month",
            "2025-02-01T00:00:00Z", true, spring);
        BlogPost draft = postAt("Matching draft", "matching-draft",
            "2025-01-17T12:00:00Z", false, spring);
        blogPostRepository
            .saveAllAndFlush(List.of(matching, wrongTag, wrongMonth, draft));

        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .param("year", "2025")
                .param("month", "1")
                .param("tag", "spring"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .model()
                .attribute("filters", hasProperty("tag", is("spring"))))
            .andExpect(MockMvcResultMatchers
                .model()
                .attribute("filters", hasProperty("year", is(2025))))
            .andExpect(MockMvcResultMatchers
                .model()
                .attribute("filters", hasProperty("month", is(1))))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("Matching post")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("Wrong tag"))))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("Wrong month"))))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("Matching draft"))));

        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .param("year", "2025")
                .param("month", "1")
                .param("tag", "spring")
                .param("query", "Matching"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("value=\"Matching\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "type=\"hidden\" name=\"tag\" value=\"spring\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "type=\"hidden\" name=\"year\" value=\"2025\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString(
                    "type=\"hidden\" name=\"month\" value=\"1\"")))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(not(containsString("postQuery"))))
            .andExpect(result -> {
                Document page = Jsoup
                    .parse(result.getResponse().getContentAsString());
                // Clicking the selected month clears only the month.
                assertThat(page
                    .select(
                        ".archive-nav__month-link.archive-nav__selected-link")
                    .eachAttr("href"))
                    .containsExactly("/?year=2025&tag=spring&query=Matching");
                // Clearing the year also clears its dependent month.
                assertThat(page
                    .select(
                        ".archive-nav__year-link.archive-nav__selected-link")
                    .eachAttr("href"))
                    .singleElement()
                    .asString()
                    .contains("tag=spring", "query=Matching")
                    .doesNotContain("year=", "month=");
                assertThat(page
                    .select(
                        ".archive-nav__month-link:not(.archive-nav__selected-link)")
                    .eachAttr("href"))
                    .contains("/?year=2025&month=2&tag=spring&query=Matching");
            });
    }

    @Test
    void htmxFilteringReplacesBlogContentAndPreservesFilters()
        throws Exception {
        List<Tag> tags = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tags
                .add(tagRepository
                    .save(new Tag("Spring %02d".formatted(i), "spring-" + i)));
        }
        BlogPost matching = postAt("January article", "january-article",
            "2025-01-15T12:00:00Z", true, tags.get(10));
        BlogPost february = postAt("February article", "february-article",
            "2025-02-15T12:00:00Z", true, tags.get(10));
        blogPostRepository.saveAllAndFlush(List.of(matching, february));

        var result = mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .header("HX-Request", "true")
                .param("year", "2025")
                .param("month", "1")
                .param("tag", "spring-10")
                .param("query", "Article")
                .param("tagQuery", "Spring")
                .param("tagPage", "1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .handler()
                .handlerType(PostListController.class))
            .andExpect(
                MockMvcResultMatchers.view().name("partials/blog-content"))
            .andExpect(MockMvcResultMatchers
                .header()
                .string(HttpHeaders.VARY, containsString("HX-Request")))
            .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(html)
            .doesNotContain("<!DOCTYPE", "<html", "<head>", "<script",
                "Loading tags…");
        Document fragment = Jsoup.parse(html);
        assertThat(fragment.select("#blogs > .post-preview")).hasSize(1);
        assertThat(fragment.select("#blogs").text())
            .contains("January article")
            .doesNotContain("February article");
        assertThat(fragment.body().children()).hasSize(1);
        assertThat(fragment.body().child(0).id()).isEqualTo("blog-content");
        assertThat(fragment.select("#post-filters, #tag-filter")).hasSize(2);
        assertThat(fragment.select("[hx-swap-oob]")).isEmpty();
        assertThat(fragment.select("#tag-filter-query").val())
            .isEqualTo("Spring");
        assertThat(fragment.select("#tag-filter").attr("hx-trigger"))
            .doesNotContain("load");
        assertThat(fragment.select("#post-filters [name=tag]").val())
            .isEqualTo("spring-10");
        assertThat(fragment.select("#tag-filter [name=query]").val())
            .isEqualTo("Article");
        assertThat(fragment.select("#tag-page-state").val()).isEqualTo("1");
        assertThat(fragment.select("#tag-list .tag-filter__link")).hasSize(1);
        assertThat(fragment.select("#tag-list [aria-current=true]").text())
            .contains("Spring 10", "clear tag filter");

        // Follow the rendered month-clear link, preserving the tag browser
        // state.
        String clearMonth = fragment
            .select(".archive-nav__month-link.archive-nav__selected-link")
            .attr("href");
        var cleared = mockMvc
            .perform(MockMvcRequestBuilders
                .get(clearMonth)
                .header("HX-Request", "true")
                .param("tagQuery", "Spring")
                .param("tagPage", "1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .handler()
                .handlerType(PostListController.class))
            .andExpect(
                MockMvcResultMatchers.view().name("partials/blog-content"))
            .andReturn();
        Document clearedFragment = Jsoup
            .parse(cleared.getResponse().getContentAsString());
        assertThat(clearedFragment.select("#blogs > .post-preview")).hasSize(2);
        assertThat(clearedFragment.select("#post-filters [name=month]"))
            .isEmpty();
        assertThat(clearedFragment.select("#post-filters [name=year]").val())
            .isEqualTo("2025");
        assertThat(clearedFragment.select("#tag-page-state").val())
            .isEqualTo("1");
    }

    @Test
    void htmxInfiniteScrollReturnsOnlyNextPostCards() throws Exception {
        Tag tag = tagRepository.save(new Tag("Java", "java"));
        List<BlogPost> posts = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            posts
                .add(postAt("Article " + i, "article-" + i,
                    "2025-01-%02dT12:00:00Z".formatted(i + 1), true, tag));
        }
        blogPostRepository.saveAllAndFlush(posts);
        var result = mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .header("HX-Request", "true")
                .param("page", "1")
                .param("tag", "java")
                .param("year", "2025")
                .param("month", "1")
                .param("query", "Article"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers
                .handler()
                .handlerType(PostListController.class))
            .andExpect(MockMvcResultMatchers.view().name("partials/post-items"))
            .andExpect(MockMvcResultMatchers
                .model()
                .attributeDoesNotExist("archiveMonths", "tags"))
            .andReturn();
        Document fragment = Jsoup
            .parse(result.getResponse().getContentAsString());
        assertThat(fragment.body().children()).hasSize(1);
        assertThat(fragment.select(".post-preview").text())
            .contains("Article 0");
        assertThat(fragment.select("main, [hx-swap-oob], [hx-get]")).isEmpty();

        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .header("HX-Request", "true")
                .param("page", "2"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(resultAfterLast -> assertThat(
                resultAfterLast.getResponse().getContentAsString()).isBlank());
    }

    @Test
    void historyRestorationReturnsFullPageAndNormalNavigationKeepsWorking()
        throws Exception {
        for (boolean htmx : List.of(false, true)) {
            var result = mockMvc
                .perform(MockMvcRequestBuilders
                    .get("/")
                    .header("HX-Request", Boolean.toString(htmx))
                    .header("HX-History-Restore-Request", "true")
                    .param("page", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers
                    .handler()
                    .handlerType(HomeController.class))
                .andExpect(MockMvcResultMatchers.view().name("index"))
                .andReturn();
            String html = result.getResponse().getContentAsString();
            assertThat(html)
                .contains("<!DOCTYPE HTML>")
                .doesNotContain("hx-swap-oob", "hx-select", "Loading tags…");
            Document page = Jsoup.parse(html);
            assertThat(page.select("#blogs, #tag-filter, #post-filters"))
                .hasSize(3);
            assertThat(page.select("#post-filters").attr("hx-target"))
                .isEqualTo("#blog-content");
            assertThat(page.select("#tag-filter").attr("hx-trigger"))
                .doesNotContain("load");
        }
    }

    @Test
    void bothControllersRejectMonthWithoutYear() throws Exception {
        for (boolean htmx : List.of(false, true)) {
            mockMvc
                .perform(MockMvcRequestBuilders
                    .get("/")
                    .header("HX-Request", Boolean.toString(htmx))
                    .param("month", "1"))
                .andExpect(MockMvcResultMatchers
                    .handler()
                    .handlerType(
                        htmx ? PostListController.class : HomeController.class))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Test
    void negativeFragmentPageReplacesListInsteadOfAppending() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/")
                .header("HX-Request", "true")
                .header("HX-History-Restore-Request", "false")
                .param("page", "-1")
                .param("tagPage", "-1"))
            .andExpect(MockMvcResultMatchers
                .handler()
                .handlerType(PostListController.class))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.view().name("partials/blog-content"))
            .andExpect(MockMvcResultMatchers
                .model()
                .attributeDoesNotExist("appendPosts", "updateFilters"))
            .andExpect(MockMvcResultMatchers.model().attribute("nextPage", 1))
            .andExpect(MockMvcResultMatchers.model().attribute("tagPage", 0))
            .andExpect(MockMvcResultMatchers
                .content()
                .string(containsString("<main id=\"blogs\">")));
    }

    private BlogPost postAt(String title, String slug, String createdAt,
        boolean published, Tag tag) {
        BlogPost post = new BlogPost(title, slug, "<p>Article content.</p>");
        post.setCreatedAt(Instant.parse(createdAt));
        post.setPublished(published);
        post.addTag(tag);
        return post;
    }

    private BlogPost savePost(String title, String slug, boolean published) {
        BlogPost post = new BlogPost(title, slug, "<p>Article content.</p>");
        post.setPublished(published);
        return blogPostRepository.save(post);
    }

    private record TestTagLink(String name, String slug,
        long postCount) implements TagLink {

        @Override
        public String getSlug() {
            return slug;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getPostCount() {
            return postCount;
        }
    }

}
