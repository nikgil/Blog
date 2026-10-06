package dev.sirnik.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Pins the contract of {@link AuthenticationUtils#isValidAdmin}: an admin is an
 * authenticated principal holding the authority {@code ROLE_ADMIN}, which is
 * what AdminUserService stores and what {@code hasRole("ADMIN")} in
 * WebSecurityConfig accepts. Authorities are spelled out as literals on purpose
 * so these tests do not depend on how AuthenticationUtils names its constant.
 */
class AuthenticationUtilsTests {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    @Test
    void nullAuthenticationIsNotAnAdmin() {
        assertThat(AuthenticationUtils.isValidAdmin(null)).isFalse();
    }

    @Test
    void unauthenticatedTokenIsNotAnAdmin() {
        Authentication auth = UsernamePasswordAuthenticationToken
            .unauthenticated("admin", "password");

        assertThat(AuthenticationUtils.isValidAdmin(auth)).isFalse();
    }

    @Test
    void adminAuthorityOnAnUntrustedTokenIsNotEnough() {
        Authentication auth = authenticatedWith(ADMIN_AUTHORITY);
        auth.setAuthenticated(false);

        assertThat(AuthenticationUtils.isValidAdmin(auth)).isFalse();
    }

    @Test
    void anonymousTokenIsNotAnAdmin() {
        Authentication auth = new AnonymousAuthenticationToken("key",
            "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        // Anonymous tokens report isAuthenticated() == true, so only the
        // authority check keeps them out.
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(AuthenticationUtils.isValidAdmin(auth)).isFalse();
    }

    @Test
    void authenticatedUserWithNoAuthoritiesIsNotAnAdmin() {
        assertThat(AuthenticationUtils.isValidAdmin(authenticatedWith()))
            .isFalse();
    }

    @Test
    void authenticatedUserWithOnlyTheUserRoleIsNotAnAdmin() {
        assertThat(
            AuthenticationUtils.isValidAdmin(authenticatedWith("ROLE_USER")))
            .isFalse();
    }

    @Test
    void authenticatedUserWithTheAdminRoleIsAnAdmin() {
        assertThat(AuthenticationUtils
            .isValidAdmin(authenticatedWith(ADMIN_AUTHORITY))).isTrue();
    }

    @Test
    void adminRoleAmongOtherAuthoritiesIsStillAnAdmin() {
        assertThat(AuthenticationUtils
            .isValidAdmin(
                authenticatedWith("ROLE_USER", ADMIN_AUTHORITY, "posts:write")))
            .isTrue();
    }

    @Test
    void userBuiltTheWayAdminUserServiceBuildsThemIsAnAdmin() {
        UserDetails details = User
            .withUsername("owner")
            .password("{noop}password")
            .roles("ADMIN")
            .build();
        Authentication auth = UsernamePasswordAuthenticationToken
            .authenticated(details, null, details.getAuthorities());

        assertThat(AuthenticationUtils.isValidAdmin(auth)).isTrue();
    }

    @Test
    void anyGrantedAuthorityImplementationCountsByItsAuthorityString() {
        GrantedAuthority custom = () -> ADMIN_AUTHORITY;
        Authentication auth = UsernamePasswordAuthenticationToken
            .authenticated("admin", "password", List.of(custom));

        assertThat(AuthenticationUtils.isValidAdmin(auth)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "role_admin", "ROLE_ROLE_ADMIN",
        "ROLE_ADMINISTRATOR", "ROLE_SUPERADMIN", "ADMIN_VIEWER"})
    void lookalikeAuthoritiesAreNotAdmin(String authority) {
        assertThat(
            AuthenticationUtils.isValidAdmin(authenticatedWith(authority)))
            .isFalse();
    }

    private static Authentication authenticatedWith(String... authorities) {
        return UsernamePasswordAuthenticationToken
            .authenticated("someone", "password",
                AuthorityUtils.createAuthorityList(authorities));
    }
}
