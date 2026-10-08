package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import dev.sirnik.blog.controllers.PostController;
import dev.sirnik.blog.controllers.PostListController;
import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.projections.ArchiveMonth;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.repositories.BlogPostPredicates;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.services.AdminUserService;
import dev.sirnik.blog.utils.AuthenticationUtils;
import jakarta.persistence.EntityManager;

/**
 * Who may do what, end to end: the filter chain in WebSecurityConfig (who may
 * POST the publish toggle) and AuthenticationUtils/AuthenticationModelAdvice
 * (who is shown admin UI), exercised with visitors, non-admins and admins.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAuthorizationTests {

    private static final String SLUG = "toggle-post";
    private static final String PUBLISH_URL = "/posts/" + SLUG + "/publish";
    private static final String ADMIN_USERNAME = "owner";
    private static final String ADMIN_EMAIL = "test@test.xxx";
    private static final String ADMIN_PASSWORD = "correct horse battery";

    private final MockMvc mockMvc;
    private final BlogPostRepository blogPostRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminUserService adminUserService;
    private final PasswordEncoder passwordEncoder;
    private final PostController postController;
    private final EntityManager entityManager;

    @Autowired
    AdminAuthorizationTests(
        MockMvc mockMvc,
        BlogPostRepository blogPostRepository,
        AdminUserRepository adminUserRepository,
        AdminUserService adminUserService,
        PasswordEncoder passwordEncoder,
        PostController postController,
        EntityManager entityManager
    ) {
        this.mockMvc = mockMvc;
        this.blogPostRepository = blogPostRepository;
        this.adminUserRepository = adminUserRepository;
        this.adminUserService = adminUserService;
        this.passwordEncoder = passwordEncoder;
        this.postController = postController;
        this.entityManager = entityManager;
    }

    // --- Filter chain: POST /posts/{slug}/publish -------------------------

    @Test
    void visitorIsRedirectedToLoginWhenTogglingPublication() throws Exception {
        savePost(true);

        mockMvc
            .perform(post(PUBLISH_URL).with(csrf()).param("published", "false"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        assertStoredPublished(true);
    }

    @Test
    void signedInNonAdminIsForbiddenFromTogglingPublication() throws Exception {
        savePost(true);

        mockMvc
            .perform(
                post(PUBLISH_URL)
                    .with(reader())
                    .with(csrf())
                    .param("published", "false")
            )
            .andExpect(status().isForbidden());

        assertStoredPublished(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ROLE_ADMINISTRATOR", "ROLE_SUPERADMIN"})
    void lookalikeAuthorityIsForbiddenFromTogglingPublication(String authority)
        throws Exception {
        savePost(true);

        mockMvc
            .perform(
                post(PUBLISH_URL)
                    .with(
                        user("sneaky")
                            .authorities(new SimpleGrantedAuthority(authority))
                    )
                    .with(csrf())
                    .param("published", "false")
            )
            .andExpect(status().isForbidden());

        assertStoredPublished(true);
    }

    @Test
    void adminWithoutCsrfTokenIsForbiddenFromTogglingPublication()
        throws Exception {
        savePost(true);

        mockMvc
            .perform(
                post(PUBLISH_URL).with(admin()).param("published", "false")
            )
            .andExpect(status().isForbidden());

        assertStoredPublished(true);
    }

    @Test
    void adminWithCsrfTokenReachesThePublishToggleHandler() throws Exception {
        savePost(true);

        mockMvc
            .perform(
                post(PUBLISH_URL)
                    .with(admin())
                    .with(csrf())
                    .param("published", "false")
            )
            .andExpect(status().isOk())
            .andExpect(handler().handlerType(PostController.class))
            .andExpect(handler().methodName("togglePublish"))
            .andExpect(view().name("partials/publish-toggle"));
    }

    @Test
    void adminTogglingFlipsTheStoredStateBothWaysAndRendersTheNewState()
        throws Exception {
        savePost(true);

        String afterUnpublish = mockMvc
            .perform(
                post(PUBLISH_URL)
                    .with(admin())
                    .with(csrf())
                    .param("published", "false")
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertStoredPublished(false);
        assertThat(toggleButtonLabel(afterUnpublish)).isEqualTo("Publish");

        String afterRepublish = mockMvc
            .perform(
                post(PUBLISH_URL)
                    .with(admin())
                    .with(csrf())
                    .param("published", "true")
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertStoredPublished(true);
        assertThat(toggleButtonLabel(afterRepublish)).isEqualTo("Unpublish");
    }

    @Test
    void adminTogglingAnUnknownPostChangesNothingAndIsNotFound()
        throws Exception {
        savePost(true);

        mockMvc
            .perform(
                post("/posts/no-such-post/publish")
                    .with(admin())
                    .with(csrf())
                    .param("published", "false")
            )
            .andExpect(status().isNotFound());

        assertStoredPublished(true);
    }

    // --- Second line of defence: the handler checks the admin itself ------
    // Calling the controller directly skips the filter chain, so these prove
    // togglePublish does not rely on WebSecurityConfig alone.

    @Test
    void handlerRejectsMissingAuthenticationWithoutTheFilterChain() {
        savePost(true);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThatThrownBy(
            () -> postController.togglePublish(SLUG, null, model)
        )
            .isInstanceOfSatisfying(
                ResponseStatusException.class,
                e -> assertThat(e.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED)
            );

        assertThat(model.containsAttribute("post")).isFalse();
        assertStoredPublished(true);
    }

    @Test
    void handlerRejectsAuthenticatedNonAdminWithoutTheFilterChain() {
        savePost(true);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThatThrownBy(
            () -> postController
                .togglePublish(SLUG, authenticatedWith("ROLE_USER"), model)
        )
            .isInstanceOfSatisfying(
                ResponseStatusException.class,
                e -> assertThat(e.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED)
            );

        assertThat(model.containsAttribute("post")).isFalse();
        assertStoredPublished(true);
    }

    @Test
    void handlerAcceptsAdminWithoutTheFilterChain() {
        savePost(true);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = postController
            .togglePublish(SLUG, authenticatedWith("ROLE_ADMIN"), model);

        assertThat(view).isEqualTo("partials/publish-toggle");
        assertThat(model.containsAttribute("post")).isTrue();
        assertStoredPublished(false);
    }

    @Test
    void handlerReportsNotFoundForAnAdminTogglingAnUnknownPost() {
        savePost(true);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThatThrownBy(
            () -> postController
                .togglePublish(
                    "no-such-post", authenticatedWith("ROLE_ADMIN"), model
                )
        )
            .isInstanceOfSatisfying(
                ResponseStatusException.class,
                e -> assertThat(e.getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND)
            );

        assertThat(model.containsAttribute("post")).isFalse();
        assertStoredPublished(true);
    }

    // --- The "loggedIn" flag and what it reveals --------------------------

    @Test
    void adminIsFlaggedAsLoggedInOnAnyPage() throws Exception {
        mockMvc
            .perform(get("/").with(admin()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("loggedIn", true));
    }

    @Test
    void visitorIsNotFlaggedAsLoggedIn() throws Exception {
        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("loggedIn", false));
    }

    @Test
    void signedInNonAdminIsNotFlaggedAsLoggedIn() throws Exception {
        mockMvc
            .perform(get("/").with(reader()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("loggedIn", false));
    }

    @Test
    void signedInNonAdminDoesNotSeeThePublishToggle() throws Exception {
        savePost(true);

        String page = mockMvc
            .perform(get("/posts/" + SLUG).with(reader()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("loggedIn", false))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(Jsoup.parse(page).select("form.post__admin")).isEmpty();
        assertThat(Jsoup.parse(page).select("form[action$=/publish]"))
            .isEmpty();
    }

    @Test
    void publishedPostPageIsVisibleToVisitorsNonAdminsAndAdmins()
        throws Exception {
        savePost(true);

        for (MockHttpServletRequestBuilder request : List
            .of(
                get("/posts/" + SLUG), get("/posts/" + SLUG).with(reader()),
                get("/posts/" + SLUG).with(admin())
            )) {
            String page = mockMvc
                .perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            assertThat(Jsoup.parse(page).select("article.post h2").text())
                .isEqualTo("Toggle post");
        }
    }

    @Test
    void adminCanViewADraftPostAndIsOfferedToPublishIt() throws Exception {
        savePost(false);

        String page = mockMvc
            .perform(get("/posts/" + SLUG).with(admin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var form = Jsoup.parse(page).selectFirst("form.post__admin");
        assertThat(form).isNotNull();
        assertThat(form.select("button[type=submit]").text())
            .isEqualTo("Publish");
        assertThat(form.select("input[name=published]").attr("value"))
            .isEqualTo("true");
    }

    @Test
    void signedInNonAdminGetsNotFoundForADraftPost() throws Exception {
        savePost(false);

        mockMvc
            .perform(get("/posts/" + SLUG).with(reader()))
            .andExpect(status().isNotFound());
    }

    @Test
    void visitorGetsNotFoundForADraftPost() throws Exception {
        savePost(false);

        mockMvc.perform(get("/posts/" + SLUG)).andExpect(status().isNotFound());
    }

    @Test
    void adminPostPageIsNotServedWithTheVisitorCacheLifetime()
        throws Exception {
        savePost(true);

        MockHttpServletResponse response = mockMvc
            .perform(get("/posts/" + SLUG).with(admin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        assertThat(response.getHeaders(HttpHeaders.CACHE_CONTROL))
            .noneMatch(value -> value.contains("max-age=60"));
    }

    // --- Post lists: drafts are listed only for admins --------------------

    @Test
    void homePageHidesDraftsFromVisitorsAndNonAdmins() throws Exception {
        savePost(false);

        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(
                model().attribute("posts", not(hasItem(draftPreview())))
            );
        mockMvc
            .perform(get("/").with(reader()))
            .andExpect(status().isOk())
            .andExpect(
                model().attribute("posts", not(hasItem(draftPreview())))
            );
    }

    @Test
    void homePageListsDraftsForAdmins() throws Exception {
        savePost(false);

        mockMvc
            .perform(get("/").with(admin()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("posts", hasItem(draftPreview())));
    }

    @Test
    void postListFragmentHidesDraftsFromVisitorsAndNonAdmins()
        throws Exception {
        savePost(false);

        mockMvc
            .perform(get("/").header("HX-Request", "true"))
            .andExpect(status().isOk())
            .andExpect(handler().handlerType(PostListController.class))
            .andExpect(
                model().attribute("posts", not(hasItem(draftPreview())))
            );
        mockMvc
            .perform(get("/").header("HX-Request", "true").with(reader()))
            .andExpect(status().isOk())
            .andExpect(handler().handlerType(PostListController.class))
            .andExpect(
                model().attribute("posts", not(hasItem(draftPreview())))
            );
    }

    @Test
    void postListFragmentListsDraftsForAdmins() throws Exception {
        savePost(false);

        mockMvc
            .perform(get("/").header("HX-Request", "true").with(admin()))
            .andExpect(status().isOk())
            .andExpect(handler().handlerType(PostListController.class))
            .andExpect(model().attribute("posts", hasItem(draftPreview())));
    }

    @Test
    void adminSeesADraftTagInlineInTheHeadingOfOnlyTheDraftPreview()
        throws Exception {
        savePost("live-post", true);
        savePost("draft-post", false);

        String page = mockMvc
            .perform(get("/").with(admin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Document document = Jsoup.parse(page);

        Element draftHeading = document
            .selectFirst("a[href=/posts/draft-post] h2");
        assertThat(draftHeading).isNotNull();
        assertThat(draftHeading.select("span.tag").text()).isEqualTo("Draft");
        // One heading with the tag inline, separated from the title by a space.
        assertThat(draftHeading.text()).isEqualTo("Draft Post draft-post");
        assertThat(document.select("a[href=/posts/draft-post] h2")).hasSize(1);

        Element liveHeading = document
            .selectFirst("a[href=/posts/live-post] h2");
        assertThat(liveHeading).isNotNull();
        assertThat(liveHeading.select(".tag")).isEmpty();
        assertThat(liveHeading.text()).isEqualTo("Post live-post");
    }

    @Test
    void visitorsAndNonAdminsSeeNoDraftTagsInThePostList() throws Exception {
        savePost("live-post", true);
        savePost("draft-post", false);

        for (MockHttpServletRequestBuilder request : List
            .of(get("/"), get("/").with(reader()))) {
            Document document = Jsoup
                .parse(
                    mockMvc
                        .perform(request)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
                );

            assertThat(document.select("article.post-preview h2 .tag"))
                .isEmpty();
            assertThat(document.select("a[href=/posts/draft-post]")).isEmpty();
        }
    }

    @Test
    void sidebarArchiveCountsDraftsOnlyForAdmins() throws Exception {
        savePost("live-post", true);
        savePost("draft-post", false);

        assertThat(archivedPosts(mockMvc.perform(get("/")).andReturn()))
            .isEqualTo(1);
        assertThat(
            archivedPosts(mockMvc.perform(get("/").with(reader())).andReturn())
        ).isEqualTo(1);
        assertThat(
            archivedPosts(mockMvc.perform(get("/").with(admin())).andReturn())
        ).isEqualTo(2);
    }

    // --- The real admin account, not a MockMvc stand-in -------------------

    @Test
    void adminLoadedFromTheDatabaseIsRecognisedByTheUtility() {
        saveAdminUser();

        UserDetails details = adminUserService
            .loadUserByUsername(ADMIN_USERNAME);
        Authentication auth = UsernamePasswordAuthenticationToken
            .authenticated(details, null, details.getAuthorities());

        assertThat(AuthenticationUtils.isValidAdmin(auth)).isTrue();
    }

    @Test
    void formLoginAsTheAdminGrantsTheAdminRole() throws Exception {
        saveAdminUser();

        mockMvc
            .perform(
                formLogin("/login")
                    .user(ADMIN_USERNAME)
                    .password(ADMIN_PASSWORD)
            )
            .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void formLoginWithAWrongPasswordDoesNotAuthenticate() throws Exception {
        saveAdminUser();

        mockMvc
            .perform(
                formLogin("/login")
                    .user(ADMIN_USERNAME)
                    .password("not the password")
            )
            .andExpect(unauthenticated())
            .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void adminWhoLogsInThroughTheFormCanUseThePublishToggle() throws Exception {
        saveAdminUser();
        savePost(true);

        MvcResult login = mockMvc
            .perform(
                formLogin("/login")
                    .user(ADMIN_USERNAME)
                    .password(ADMIN_PASSWORD)
            )
            .andReturn();
        MockHttpSession session = (MockHttpSession) login
            .getRequest()
            .getSession(false);
        assertThat(session).isNotNull();

        String page = mockMvc
            .perform(get("/posts/" + SLUG).session(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(Jsoup.parse(page).select("form.post__admin")).hasSize(1);

        mockMvc
            .perform(
                post(PUBLISH_URL)
                    .session(session)
                    .with(csrf())
                    .param("published", "false")
            )
            .andExpect(status().isOk());
    }

    // --- Helpers ----------------------------------------------------------

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    private static RequestPostProcessor reader() {
        return user("reader").roles("USER");
    }

    private static Authentication authenticatedWith(String... authorities) {
        return UsernamePasswordAuthenticationToken
            .authenticated(
                "someone", "password",
                AuthorityUtils.createAuthorityList(authorities)
            );
    }

    private static Matcher<Object> draftPreview() {
        return hasProperty("slug", is(SLUG));
    }

    @SuppressWarnings("unchecked")
    private static long archivedPosts(MvcResult result) {
        Map<Integer, List<ArchiveMonth>> months = (Map<Integer, List<ArchiveMonth>>) result
            .getModelAndView()
            .getModel()
            .get("archiveMonths");
        return months
            .values()
            .stream()
            .flatMap(List::stream)
            .mapToLong(ArchiveMonth::getPostCount)
            .sum();
    }

    private void savePost(
        String slug,
        boolean published
    ) {
        BlogPost post = new BlogPost(
            "Post " + slug,
            slug,
            "<p>Body.</p>"
        );
        post.setPublished(published);
        blogPostRepository.saveAndFlush(post);
    }

    private static String toggleButtonLabel(String fragmentHtml) {
        return Jsoup
            .parse(fragmentHtml)
            .select("#publish-toggle-span > form#publish-toggle button")
            .text();
    }

    private void assertStoredPublished(boolean expected) {
        // Flush and clear so this reads what was written, not the managed copy.
        entityManager.flush();
        entityManager.clear();
        assertThat(
            blogPostRepository
                .findOne(BlogPostPredicates.hasSlug(SLUG))
                .orElseThrow()
                .isPublished()
        ).isEqualTo(expected);
    }

    private void savePost(boolean published) {
        BlogPost post = new BlogPost(
            "Toggle post",
            SLUG,
            "<p>Body.</p>"
        );
        post.setPublished(published);
        blogPostRepository.saveAndFlush(post);
    }

    // Login only loads activated users (findActiveByLogin), so the account
    // must be approved before form login can succeed.
    private void saveAdminUser() {
        AdminUser admin = new AdminUser(
            ADMIN_EMAIL,
            ADMIN_USERNAME,
            passwordEncoder.encode(ADMIN_PASSWORD)
        );
        admin.setActivated(true);
        adminUserRepository.saveAndFlush(admin);
    }
}
