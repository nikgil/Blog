package dev.sirnik.blog.posts;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.models.projections.BlogPostLink;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;
import jakarta.persistence.Persistence;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogPostRepositoryTests {

    private final BlogPostRepository blogPostRepository;
    private final TagRepository tagRepository;
    private final EntityManager entityManager;

    @Autowired
    BlogPostRepositoryTests(BlogPostRepository blogPostRepository,
        TagRepository tagRepository, EntityManager entityManager) {
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsPostBySlug() {
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        BlogPost post = new BlogPost("Setting up the blog",
            "setting-up-the-blog", "The first persisted post.");
        post.addTag(spring);
        post.setPublished(true);

        BlogPost savedPost = blogPostRepository.saveAndFlush(post);
        Long postId = savedPost.getId();
        entityManager.clear();

        BlogPost reloadedPost = blogPostRepository
            .findBySlugAndPublishedTrue("setting-up-the-blog")
            .orElseThrow();

        assertThat(postId).isNotNull();
        assertThat(reloadedPost.getId()).isEqualTo(postId);
        assertThat(reloadedPost.getCreatedAt()).isNotNull();
        assertThat(reloadedPost.getUpdatedAt()).isNotNull();
        assertThat(
            Persistence.getPersistenceUtil().isLoaded(reloadedPost, "tags"))
            .isTrue();
        assertThat(reloadedPost.getTags())
            .extracting(Tag::getSlug)
            .containsExactly("spring");
    }

    @Test
    void findsImmediatePublishedPostsAndSkipsDrafts() {
        BlogPost oldest = savePost("Oldest", "oldest", true);
        savePost("Older draft", "older-draft", false);
        BlogPost current = savePost("Current", "current", true);
        savePost("Newer draft", "newer-draft", false);
        BlogPost newest = savePost("Newest", "newest", true);
        blogPostRepository.flush();

        BlogPostLink older = blogPostRepository
            .findOlderPublished(current.getCreatedAt(), current.getId());
        BlogPostLink newer = blogPostRepository
            .findNewerPublished(current.getCreatedAt(), current.getId());

        assertThat(older).isNotNull();
        assertThat(older.getSlug()).isEqualTo(oldest.getSlug());
        assertThat(newer).isNotNull();
        assertThat(newer.getSlug()).isEqualTo(newest.getSlug());

        assertThat(blogPostRepository
            .findOlderPublished(oldest.getCreatedAt(), oldest.getId()))
            .isNull();
        assertThat(blogPostRepository
            .findNewerPublished(newest.getCreatedAt(), newest.getId()))
            .isNull();
    }

    @Test
    void savesManyToManyTags() {
        Tag java = tagRepository.save(new Tag("Java", "java"));
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        BlogPost post = new BlogPost("Spring persistence", "spring-persistence",
            "Using JPA with Flyway.");
        post.addTag(java);
        post.addTag(spring);

        Long postId = blogPostRepository.saveAndFlush(post).getId();
        entityManager.clear();

        BlogPost reloadedPost = blogPostRepository
            .findById(postId)
            .orElseThrow();
        Tag reloadedJava = tagRepository.findBySlug("java").orElseThrow();

        assertThat(reloadedPost.getTags())
            .extracting(Tag::getSlug)
            .containsExactlyInAnyOrder("java", "spring");
        assertThat(reloadedJava.getBlogPosts())
            .extracting(BlogPost::getId)
            .contains(postId);
    }

    private BlogPost savePost(String title, String slug, boolean published) {
        BlogPost post = new BlogPost(title, slug, "Article content.");
        post.setPublished(published);
        return blogPostRepository.save(post);
    }
}
