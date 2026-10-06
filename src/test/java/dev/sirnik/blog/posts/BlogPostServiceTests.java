package dev.sirnik.blog.posts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;
import dev.sirnik.blog.services.BlogPostService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogPostServiceTests {

    private final BlogPostService blogPostService;
    private final BlogPostRepository blogPostRepository;
    private final TagRepository tagRepository;
    private final EntityManager entityManager;

    @Autowired
    BlogPostServiceTests(BlogPostService blogPostService,
        BlogPostRepository blogPostRepository, TagRepository tagRepository,
        EntityManager entityManager) {
        this.blogPostService = blogPostService;
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
        this.entityManager = entityManager;
    }

    // --- findBySlug (visibility depends on who is asking) ------------------

    @Test
    void findBySlugLoadsTheTags() {
        saveTaggedPost("tagged", true);
        entityManager.clear();

        BlogPost found = blogPostService
            .findBySlug("tagged", null)
            .orElseThrow();

        assertThat(Persistence.getPersistenceUtil().isLoaded(found, "tags"))
            .isTrue();
        assertThat(found.getTags())
            .extracting(Tag::getSlug)
            .containsExactly("spring");
    }

    @Test
    void findBySlugLoadsTheTagsOfADraftForAnAdmin() {
        saveTaggedPost("tagged-draft", false);
        entityManager.clear();

        BlogPost found = blogPostService
            .findBySlug("tagged-draft", authenticatedWith("ROLE_ADMIN"))
            .orElseThrow();

        assertThat(Persistence.getPersistenceUtil().isLoaded(found, "tags"))
            .isTrue();
    }

    @Test
    void findBySlugShowsPublishedPostsToEveryone() {
        savePost("Live", "live", true);

        assertThat(blogPostService.findBySlug("live", null)).isPresent();
        assertThat(
            blogPostService.findBySlug("live", authenticatedWith("ROLE_USER")))
            .isPresent();
        assertThat(
            blogPostService.findBySlug("live", authenticatedWith("ROLE_ADMIN")))
            .isPresent();
    }

    @Test
    void findBySlugHidesDraftsFromVisitors() {
        savePost("Draft", "a-draft", false);

        assertThat(blogPostService.findBySlug("a-draft", null)).isEmpty();
    }

    @Test
    void findBySlugHidesDraftsFromSignedInNonAdmins() {
        savePost("Draft", "a-draft", false);

        assertThat(blogPostService
            .findBySlug("a-draft", authenticatedWith("ROLE_USER"))).isEmpty();
    }

    @Test
    void findBySlugHidesDraftsFromAnAdminTokenThatIsNotAuthenticated() {
        savePost("Draft", "a-draft", false);
        Authentication untrusted = authenticatedWith("ROLE_ADMIN");
        untrusted.setAuthenticated(false);

        assertThat(blogPostService.findBySlug("a-draft", untrusted)).isEmpty();
    }

    @Test
    void findBySlugShowsDraftsToAdmins() {
        BlogPost draft = savePost("Draft", "a-draft", false);

        Optional<BlogPost> found = blogPostService
            .findBySlug("a-draft", authenticatedWith("ROLE_ADMIN"));

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(draft.getId());
        assertThat(found.get().isPublished()).isFalse();
    }

    @Test
    void findBySlugReturnsOnlyTheRequestedPost() {
        savePost("First", "first", true);
        BlogPost second = savePost("Second", "second", true);

        assertThat(blogPostService.findBySlug("second", null))
            .map(BlogPost::getId)
            .contains(second.getId());
        assertThat(blogPostService
            .findBySlug("second", authenticatedWith("ROLE_ADMIN")))
            .map(BlogPost::getId)
            .contains(second.getId());
    }

    @Test
    void findBySlugOfUnknownOrBlankSlugIsEmptyEvenForAnAdmin() {
        savePost("First", "first", true);
        savePost("Second", "second", false);
        Authentication admin = authenticatedWith("ROLE_ADMIN");

        assertThat(blogPostService.findBySlug("missing", null)).isEmpty();
        assertThat(blogPostService.findBySlug("", null)).isEmpty();
        // Admins skip the published-only restriction, so a blank slug must
        // not widen the lookup to every post.
        assertThat(blogPostService.findBySlug("missing", admin)).isEmpty();
        assertThat(blogPostService.findBySlug("", admin)).isEmpty();
    }

    // --- togglePublished --------------------------------------------------

    @Test
    void togglePublishedTurnsAPublishedPostIntoADraftWithoutAnExplicitSave() {
        BlogPost post = savePost("Live", "live", true);
        entityManager.flush();
        entityManager.clear();

        Optional<BlogPost> toggled = blogPostService.togglePublished("live");

        assertThat(toggled).isPresent();
        assertThat(toggled.get().isPublished()).isFalse();
        entityManager.flush();
        entityManager.clear();
        assertThat(blogPostRepository.findById(post.getId()).orElseThrow())
            .extracting(BlogPost::isPublished)
            .isEqualTo(false);
    }

    @Test
    void togglePublishedPublishesADraft() {
        BlogPost post = savePost("Draft", "a-draft", false);
        entityManager.flush();
        entityManager.clear();

        Optional<BlogPost> toggled = blogPostService.togglePublished("a-draft");

        assertThat(toggled).isPresent();
        assertThat(toggled.get().isPublished()).isTrue();
        entityManager.flush();
        entityManager.clear();
        assertThat(blogPostRepository.findById(post.getId()).orElseThrow())
            .extracting(BlogPost::isPublished)
            .isEqualTo(true);
    }

    @Test
    void togglePublishedTwiceRestoresTheOriginalState() {
        savePost("Live", "live", true);

        blogPostService.togglePublished("live");
        Optional<BlogPost> again = blogPostService.togglePublished("live");

        assertThat(again).isPresent();
        assertThat(again.get().isPublished()).isTrue();
    }

    @Test
    void togglePublishedReturnsThePostWithItsTagsLoaded() {
        saveTaggedPost("tagged", true);
        entityManager.clear();

        BlogPost toggled = blogPostService
            .togglePublished("tagged")
            .orElseThrow();

        assertThat(Persistence.getPersistenceUtil().isLoaded(toggled, "tags"))
            .isTrue();
    }

    @Test
    void togglePublishedOfUnknownOrBlankSlugChangesNothing() {
        BlogPost first = savePost("First", "first", true);
        BlogPost second = savePost("Second", "second", false);
        entityManager.flush();
        entityManager.clear();

        assertThat(blogPostService.togglePublished("missing")).isEmpty();
        assertThat(blogPostService.togglePublished("")).isEmpty();

        entityManager.flush();
        entityManager.clear();
        assertThat(blogPostRepository.findById(first.getId()).orElseThrow())
            .extracting(BlogPost::isPublished)
            .isEqualTo(true);
        assertThat(blogPostRepository.findById(second.getId()).orElseThrow())
            .extracting(BlogPost::isPublished)
            .isEqualTo(false);
    }

    // --- Helpers ----------------------------------------------------------

    private static Authentication authenticatedWith(String... authorities) {
        return UsernamePasswordAuthenticationToken
            .authenticated("someone", "password",
                AuthorityUtils.createAuthorityList(authorities));
    }

    private BlogPost savePost(String title, String slug, boolean published) {
        BlogPost post = new BlogPost(title, slug, "Article content.");
        post.setPublished(published);
        return blogPostRepository.save(post);
    }

    private void saveTaggedPost(String slug, boolean published) {
        Tag spring = tagRepository.save(new Tag("Spring", "spring"));
        BlogPost post = new BlogPost("Tagged", slug, "Article content.");
        post.addTag(spring);
        post.setPublished(published);
        blogPostRepository.saveAndFlush(post);
    }
}
