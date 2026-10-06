package dev.sirnik.blog.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AuthenticationUtils {

    public static final GrantedAuthority ADMIN_ROLE = new SimpleGrantedAuthority(
        "ADMIN"
    );

    private static final GrantedAuthority ADMIN_ROLE_FOR_CHECK = new SimpleGrantedAuthority(
        "ROLE_" + ADMIN_ROLE.getAuthority()
    );

    private AuthenticationUtils() {
    }

    public static boolean isValidAdmin(Authentication auth) {
        if (auth == null) {
            return false;
        }

        if (!auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().contains(ADMIN_ROLE_FOR_CHECK);
    }
}
