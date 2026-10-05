package dev.sirnik.blog.services;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;

/**
 * Spring Security calls loadUserByUsername during form login, then compares the
 * submitted password with the stored hash using the PasswordEncoder bean.
 */
@Service
public class AdminUserService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public AdminUserService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "No admin user named " + username));

        return User
            .withUsername(adminUser.getUsername())
            .password(adminUser.getPasswordHash())
            .roles("ADMIN")
            .build();
    }
}
