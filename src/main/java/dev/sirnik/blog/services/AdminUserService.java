package dev.sirnik.blog.services;

import java.util.Optional;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.utils.AuthenticationUtils;

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
            .roles(AuthenticationUtils.ADMIN_ROLE.getAuthority())
            .build();
    }

    @Transactional
    @EventListener
    public void updateTimeStamps(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();

        if (auth == null) {
            return;
        }

        Optional<AdminUser> userOpt = adminUserRepository
            .findByUsername(auth.getName());

        if (!userOpt.isPresent()) {
            return;
        }

        AdminUser user = userOpt.get();

        user.updateTimeStampsToNow();

        adminUserRepository.save(user);
    }
}
