package dev.sirnik.blog.posts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.models.filters.PostFilters;
import dev.sirnik.blog.models.projections.ArchiveMonth;
import dev.sirnik.blog.models.views.BlogPostPreviewView;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;
import dev.sirnik.blog.services.BlogPostPreviewService;

/**
 * Who sees drafts in the post list and the archive: only a valid admin. Every
 * other filter must keep working the same way for admins.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogPostPreviewServiceTests {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 10);

    private final BlogPostPreviewService previewService;
    private final BlogPostRepository blogPostRepository;
    private final TagRepository tagRepository;

    @Autowired
    BlogPostPreviewServiceTests(
        BlogPostPreviewService previewService,
        BlogPostRepository blogPostRepository,
        TagRepository tagRepository
    ) {
        this.previewService = previewService;
        this.blogPostRepository = blogPostRepository;
        this.tagRepository = tagRepository;
    }

    // --- findPreviews -----------------------------------------------------

    @Test
    void previewsHideDraftsFromVisitors() {
        savePost("live", "Live post", true);
        savePost("a-draft", "Draft post", false);

        assertThat(previewSlugs(null)).containsExactly("live");
    }

    @Test
    void previewsHideDraftsFromSignedInNonAdmins() {
        savePost("live", "Live post", true);
        savePost("a-draft", "Draft post", false);

        assertThat(previewSlugs(authenticatedWith("ROLE_USER")))
            .containsExactly("live");
    }

    @Test
    void previewsHideDraftsFromAnAdminTokenThatIsNotAuthenticated() {
        savePost("live", "Live post", true);
        savePost("a-draft", "Draft post", false);
        Authentication untrusted = authenticatedWith("ROLE_ADMIN");
        untrusted.setAuthenticated(false);

        assertThat(previewSlugs(untrusted)).containsExactly("live");
    }

    @Test
    void previewsIncludeDraftsForAdmins() {
        savePost("live", "Live post", true);
        savePost("a-draft", "Draft post", false);

        assertThat(previewSlugs(authenticatedWith("ROLE_ADMIN")))
            .containsExactlyInAnyOrder("live", "a-draft");
    }

    @Test
    void adminsStillGetTheTagFilterApplied() {
        Tag spring = tagRepository
            .save(
                new Tag(
                    "Spring",
                    "spring"
                )
            );
        saveTaggedPost("tagged-live", true, spring);
        saveTaggedPost("tagged-draft", false, spring);
        savePost("plain-draft", "Plain draft", false);

        PostFilters filters = new PostFilters();
        filters.setTag("spring");

        assertThat(previewSlugs(filters, authenticatedWith("ROLE_ADMIN")))
            .containsExactlyInAnyOrder("tagged-live", "tagged-draft");
        assertThat(previewSlugs(filters, null)).containsExactly("tagged-live");
    }

    @Test
    void adminsStillGetTheSearchQueryApplied() {
        blogPostRepository
            .save(
                new BlogPost(
                    "Needle draft",
                    "needle-draft",
                    "<p>Has a needle inside.</p>"
                )
            );
        blogPostRepository
            .save(
                new BlogPost(
                    "Other draft",
                    "other-draft",
                    "<p>Nothing relevant.</p>"
                )
            );

        PostFilters filters = new PostFilters();
        filters.setQuery("needle");

        assertThat(previewSlugs(filters, authenticatedWith("ROLE_ADMIN")))
            .containsExactly("needle-draft");
        assertThat(previewSlugs(filters, null)).isEmpty();
    }

    // --- getArchiveMonths -------------------------------------------------

    @Test
    void archiveCountsExcludeDraftsForVisitorsAndNonAdmins() {
        savePost("live", "Live post", true);
        savePost("draft-one", "Draft one", false);
        savePost("draft-two", "Draft two", false);

        assertThat(archivedPosts(null)).isEqualTo(1);
        assertThat(archivedPosts(authenticatedWith("ROLE_USER"))).isEqualTo(1);
    }

    @Test
    void archiveCountsIncludeDraftsForAdmins() {
        savePost("live", "Live post", true);
        savePost("draft-one", "Draft one", false);
        savePost("draft-two", "Draft two", false);

        assertThat(archivedPosts(authenticatedWith("ROLE_ADMIN"))).isEqualTo(3);
    }

    // --- Helpers ----------------------------------------------------------

    private List<String> previewSlugs(Authentication auth) {
        return previewSlugs(new PostFilters(), auth);
    }

    private List<String> previewSlugs(
        PostFilters filters,
        Authentication auth
    ) {
        return previewService
            .findPreviews(FIRST_PAGE, filters, auth)
            .getContent()
            .stream()
            .map(BlogPostPreviewView::getSlug)
            .toList();
    }

    private long archivedPosts(Authentication auth) {
        return previewService
            .getArchiveMonths(auth)
            .stream()
            .mapToLong(ArchiveMonth::getPostCount)
            .sum();
    }

    private static Authentication authenticatedWith(String... authorities) {
        return UsernamePasswordAuthenticationToken
            .authenticated(
                "someone", "password",
                AuthorityUtils.createAuthorityList(authorities)
            );
    }

    private void savePost(
        String slug,
        String title,
        boolean published
    ) {
        BlogPost post = new BlogPost(
            title,
            slug,
            "<p>Article content.</p>"
        );
        post.setPublished(published);
        blogPostRepository.save(post);
    }

    private void saveTaggedPost(
        String slug,
        boolean published,
        Tag tag
    ) {
        BlogPost post = new BlogPost(
            "Tagged " + slug,
            slug,
            "<p>Article content.</p>"
        );
        post.addTag(tag);
        post.setPublished(published);
        blogPostRepository.save(post);
    }
}
