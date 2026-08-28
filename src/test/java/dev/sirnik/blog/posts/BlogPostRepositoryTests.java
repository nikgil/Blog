package dev.sirnik.blog.posts;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogPostRepositoryTests {

    private final BlogPostRepository blogPostRepository;
    private final TagRepository tagRepository;
    private final EntityManager entityManager;

    @Autowired
    BlogPostRepositoryTests(
            BlogPostRepository blogPostRepository,
            TagRepository tagRepository,
            EntityManager entityManager
    ) {
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsPostBySlug() {
        BlogPost post = new BlogPost(
                "Setting up the blog",
                "setting-up-the-blog",
                "The first persisted post."
        );

        BlogPost savedPost = blogPostRepository.saveAndFlush(post);

        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getUpdatedAt()).isNotNull();
        assertThat(blogPostRepository.findBySlug("setting-up-the-blog"))
                .contains(savedPost);
    }

    @Test
    void savesManyToManyTags() {
        Tag java = tagRepository.save(new Tag("Java", "java"));
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        BlogPost post = new BlogPost(
                "Spring persistence",
                "spring-persistence",
                "Using JPA with Flyway."
        );
        post.addTag(java);
        post.addTag(spring);

        Long postId = blogPostRepository.saveAndFlush(post).getId();
        entityManager.clear();

        BlogPost reloadedPost = blogPostRepository.findById(postId).orElseThrow();
        Tag reloadedJava = tagRepository.findBySlug("java").orElseThrow();

        assertThat(reloadedPost.getTags())
                .extracting(Tag::getSlug)
                .containsExactlyInAnyOrder("java", "spring");
        assertThat(reloadedJava.getBlogPosts())
                .extracting(BlogPost::getId)
                .contains(postId);
    }
}
