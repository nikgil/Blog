package dev.sirnik.blog.tags;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.models.projections.TagLink;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TagRepositoryTests {

    private final BlogPostRepository blogPostRepository;
    private final TagRepository tagRepository;

    @Autowired
    TagRepositoryTests(BlogPostRepository blogPostRepository,
        TagRepository tagRepository) {
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
    }

    @Test
    void tagLinksCountOnlyPublishedPostsAndIncludeUnusedTags() {
        Tag java = tagRepository.save(new Tag("Java", "java"));
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        Tag springBoot = tagRepository
            .save(new Tag("Spring Boot", "spring-boot"));
        tagRepository.save(new Tag("Spring Cloud", "spring-cloud"));

        savePost("Published Spring", "published-spring", true, spring,
            springBoot);
        savePost("Another Boot post", "another-boot-post", true, springBoot);
        savePost("Spring draft", "spring-draft", false, spring);
        savePost("Published Java", "published-java", true, java);
        blogPostRepository.flush();

        Slice<TagLink> result = tagRepository
            .findTagLinks("sPrInG", PageRequest.of(0, 10));

        assertThat(result.getContent())
            .extracting(tag -> tag.getName() + ":" + tag.getPostCount())
            .containsExactly("Spring:1", "Spring Boot:2", "Spring Cloud:0");
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void tagLinksUseStableSlicePaging() {
        List<Tag> tags = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tags.add(new Tag("Tag %02d".formatted(i), "tag-%02d".formatted(i)));
        }
        tagRepository.saveAllAndFlush(tags);

        Slice<TagLink> firstPage = tagRepository
            .findTagLinks("", PageRequest.of(0, 10));
        Slice<TagLink> secondPage = tagRepository
            .findTagLinks("", PageRequest.of(1, 10));

        assertThat(firstPage.getContent())
            .extracting(TagLink::getName)
            .containsExactly("Tag 00", "Tag 01", "Tag 02", "Tag 03", "Tag 04",
                "Tag 05", "Tag 06", "Tag 07", "Tag 08", "Tag 09");
        assertThat(firstPage.hasPrevious()).isFalse();
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent())
            .extracting(TagLink::getName)
            .containsExactly("Tag 10");
        assertThat(secondPage.hasPrevious()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }

    private void savePost(String title, String slug, boolean published,
        Tag... tags) {
        BlogPost post = new BlogPost(title, slug, "Article content.");
        post.setPublished(published);
        for (Tag tag : tags) {
            post.addTag(tag);
        }
        blogPostRepository.save(post);
    }
}
