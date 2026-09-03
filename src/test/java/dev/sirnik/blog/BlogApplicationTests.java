package dev.sirnik.blog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;

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
    BlogApplicationTests(
            MockMvc mockMvc,
            BlogPostRepository blogPostRepository,
            TagRepository tagRepository,
            Configuration freeMarkerConfiguration) {
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
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("index"))
                .andExpect(MockMvcResultMatchers.content()
                        .string(containsString("sirnik.Dev")));
    }

    @Test
    void notFoundPageLinksBackToHome() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI,
                        "/missing-page"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.view().name("error/404"))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("This page wandered off.")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("href=\"/\"")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("Back to all posts")));
    }

    @Test
    void genericErrorPageLinksBackToHome() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI,
                        "/failed-page"))
                .andExpect(MockMvcResultMatchers.status()
                        .isInternalServerError())
                .andExpect(MockMvcResultMatchers.view().name("error"))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("Something went wrong.")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("href=\"/\"")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("Back to all posts")));
    }

    @Test
    void postTemplateRendersPostAndAdjacentNavigation() throws Exception {
        BlogPost post = new BlogPost(
                "Rendered post",
                "rendered-post",
                "<p>Trusted article content.</p>");
        post.addTag(new Tag("Zulu", "zulu"));
        post.addTag(new Tag("Alpha", "alpha"));
        post.setCreationTimestamps();

        Map<String, Object> model = new HashMap<>();
        model.put("post", post);
        model.put("postDateFormatter",
                DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC));
        model.put("olderPost", Map.of(
                "title", "Older post",
                "slug", "older-post"));
        model.put("newerPost", Map.of(
                "title", "Newer post",
                "slug", "newer-post"));

        StringWriter rendered = new StringWriter();
        freeMarkerConfiguration.getTemplate("post.ftl")
                .process(model, rendered);

        assertThat(rendered.toString())
                .contains("Rendered post")
                .contains("<p>Trusted article content.</p>")
                .containsSubsequence("Alpha", "Zulu")
                .contains("href=\"/posts/older-post\"")
                .contains("href=\"/posts/newer-post\"")
                .contains("preload=\"mouseover\"");
    }

    @Test
    void testHomeRendersOrderedPostPreviewsAndNextSliceTrigger()
            throws Exception {
        Tag zulu = tagRepository.save(new Tag("Zulu", "zulu"));
        Tag alpha = tagRepository.save(new Tag("Alpha", "alpha"));
        List<BlogPost> posts = new ArrayList<>();

        // Eleven records make the first ten-record Slice report hasNext=true.
        // Reverse insertion proves display order comes from the query.
        for (int i = 0; i < 11; i++) {
            BlogPost post = new BlogPost(
                    "Post " + i,
                    "post-" + i,
                    "<p>Lorem ipsum preview " + i + ".</p>");
            post.addTag(zulu);
            post.addTag(alpha);
            post.setPublished(true);
            posts.add(post);
        }
        blogPostRepository.saveAllAndFlush(posts);

        mockMvc.perform(MockMvcRequestBuilders.get("/test-home")
                .param("page", "0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("index"))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("post-preview__tag-scroll")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("post-preview__body")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("class=\"post-preview__date\"")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString(
                                "post-preview__loading htmx-indicator")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("Loading more posts…")))
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("Lorem ipsum")))
                .andExpect(MockMvcResultMatchers.content().string(
                        stringContainsInOrder("Alpha", "Zulu")));

        // The last Slice has no successor, so it must not render a new trigger.
        mockMvc.perform(MockMvcRequestBuilders.get("/test-home")
                .param("page", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(
                        containsString("Post 0")))
                .andExpect(MockMvcResultMatchers.content().string(not(
                        containsString("post-preview__loading"))));
    }

}
