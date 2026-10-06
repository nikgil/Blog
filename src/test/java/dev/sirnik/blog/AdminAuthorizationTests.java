package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
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

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;

import dev.sirnik.blog.controllers.PostController;
import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.services.AdminUserService;
import dev.sirnik.blog.utils.AuthenticationUtils;

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
    private static final String ADMIN_PASSWORD = "correct horse battery";

    private final MockMvc mockMvc;
    private final BlogPostRepository blogPostRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminUserService adminUserService;
    private final PasswordEncoder passwordEncoder;
    private final PostController postController;

    @Autowired
    AdminAuthorizationTests(MockMvc mockMvc,
        BlogPostRepository blogPostRepository,
        AdminUserRepository adminUserRepository,
        AdminUserService adminUserService, PasswordEncoder passwordEncoder,
        PostController postController) {
        this.mockMvc = mockMvc;
        this.blogPostRepository = blogPostRepository;
        this.adminUserRepository = adminUserRepository;
        this.adminUserService = adminUserService;
        this.passwordEncoder = passwordEncoder;
        this.postController = postController;
    }

    // --- Filter chain: POST /posts/{slug}/publish -------------------------

    @Test
    void visitorIsRedirectedToLoginWhenTogglingPublication() throws Exception {
        mockMvc
            .perform(post(PUBLISH_URL).with(csrf()).param("published", "false"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void signedInNonAdminIsForbiddenFromTogglingPublication() throws Exception {
        mockMvc
            .perform(post(PUBLISH_URL)
                .with(reader())
                .with(csrf())
                .param("published", "false"))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ROLE_ADMINISTRATOR", "ROLE_SUPERADMIN"})
    void lookalikeAuthorityIsForbiddenFromTogglingPublication(String authority)
        throws Exception {
        mockMvc
            .perform(post(PUBLISH_URL)
                .with(user("sneaky")
                    .authorities(new SimpleGrantedAuthority(authority)))
                .with(csrf())
                .param("published", "false"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminWithoutCsrfTokenIsForbiddenFromTogglingPublication()
        throws Exception {
        mockMvc
            .perform(
                post(PUBLISH_URL).with(admin()).param("published", "false"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminWithCsrfTokenReachesThePublishToggleHandler() throws Exception {
        savePost(true);

        mockMvc
            .perform(post(PUBLISH_URL)
                .with(admin())
                .with(csrf())
                .param("published", "false"))
            .andExpect(status().isOk())
            .andExpect(handler().handlerType(PostController.class))
            .andExpect(handler().methodName("togglePublish"))
            .andExpect(view().name("partials/publish-toggle"));
    }

    // --- Second line of defence: the handler checks the admin itself ------
    // Calling the controller directly skips the filter chain, so these prove
    // togglePublish does not rely on WebSecurityConfig alone.

    @Test
    void handlerRejectsMissingAuthenticationWithoutTheFilterChain() {
        savePost(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        postController.togglePublish(SLUG, null, response, model);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(model.containsAttribute("post")).isFalse();
    }

    @Test
    void handlerRejectsAuthenticatedNonAdminWithoutTheFilterChain() {
        savePost(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        postController
            .togglePublish(SLUG, authenticatedWith("ROLE_USER"), response,
                model);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(model.containsAttribute("post")).isFalse();
    }

    @Test
    void handlerAcceptsAdminWithoutTheFilterChain() {
        savePost(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = postController
            .togglePublish(SLUG, authenticatedWith("ROLE_ADMIN"), response,
                model);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(view).isEqualTo("partials/publish-toggle");
        assertThat(model.containsAttribute("post")).isTrue();
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
            .perform(formLogin("/login")
                .user(ADMIN_USERNAME)
                .password(ADMIN_PASSWORD))
            .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void formLoginWithAWrongPasswordDoesNotAuthenticate() throws Exception {
        saveAdminUser();

        mockMvc
            .perform(formLogin("/login")
                .user(ADMIN_USERNAME)
                .password("not the password"))
            .andExpect(unauthenticated())
            .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void adminWhoLogsInThroughTheFormCanUseThePublishToggle() throws Exception {
        saveAdminUser();
        savePost(true);

        MvcResult login = mockMvc
            .perform(formLogin("/login")
                .user(ADMIN_USERNAME)
                .password(ADMIN_PASSWORD))
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
            .perform(post(PUBLISH_URL)
                .session(session)
                .with(csrf())
                .param("published", "false"))
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
            .authenticated("someone", "password",
                AuthorityUtils.createAuthorityList(authorities));
    }

    private void savePost(boolean published) {
        BlogPost post = new BlogPost("Toggle post", SLUG, "<p>Body.</p>");
        post.setPublished(published);
        blogPostRepository.saveAndFlush(post);
    }

    private void saveAdminUser() {
        adminUserRepository
            .saveAndFlush(new AdminUser(ADMIN_USERNAME,
                passwordEncoder.encode(ADMIN_PASSWORD)));
    }
}
