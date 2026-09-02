package dev.sirnik.blog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

    @Autowired
    BlogApplicationTests(
            MockMvc mockMvc,
            BlogPostRepository blogPostRepository,
            TagRepository tagRepository) {
        this.mockMvc = mockMvc;
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
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
