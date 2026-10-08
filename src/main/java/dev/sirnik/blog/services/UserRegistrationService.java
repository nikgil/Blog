package dev.sirnik.blog.services;

import java.time.Instant;
import java.util.Optional;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jose.JOSEException;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.models.forms.RegistrationForm;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.utils.JsonWebTokenUtils;
import dev.sirnik.blog.utils.JsonWebTokenUtils.JWTPayload;
import dev.sirnik.blog.utils.RegistrationConfig;

@Service
public class UserRegistrationService {

    private final AdminUserRepository adminUserRepository;
    private final JavaMailSender javaMailSender;
    private final RegistrationConfig registrationConfig;

    public UserRegistrationService(
        AdminUserRepository adminUserRepository,
        JavaMailSender javaMailSender,
        RegistrationConfig registrationConfig
    ) {
        this.adminUserRepository = adminUserRepository;
        this.javaMailSender = javaMailSender;
        this.registrationConfig = registrationConfig;
    }

    public boolean isValidRegistrationForm(RegistrationForm form) {
        if (form.getEmail() == null || form.getName() == null
            || form.getPassword() == null) {
            return false;
        }

        if (adminUserRepository
            .findByUsernameOrEmail(form.getName(), form.getEmail())
            .isPresent()) {
            return false;
        }

        // min and max length for pwd with BCrypt
        if (form.getPassword().length() > 70
            || form.getPassword().length() < 3) {
            return false;
        }

        return EmailValidator.getInstance().isValid(form.getEmail());
    }

    @Async
    public void sendConfirmationLink(AdminUser user) {
        if (!registrationConfig.enabled()) {
            return;
        }

        String confirmation;
        try {
            confirmation = JsonWebTokenUtils
                .encodePayload(
                    registrationConfig.jwt(), new JWTPayload(
                        user.getUsername(),
                        user.getEmail(),
                        user.getCreatedAt().toEpochMilli()
                    )
                );
        } catch (JOSEException e) {
            return;
        }

        String fullURL = registrationConfig.url()
            + "/register/confirm?confirmation=" + confirmation;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(registrationConfig.mail().approver());
        message.setFrom(registrationConfig.mail().sender());
        message.setSubject("Approval request for " + user.getUsername());
        message
            .setText(
                "Please click the link to approve the user "
                    + user.getUsername() + " with email " + user.getEmail()
                    + ".\n" + "URL: " + fullURL
            );

        javaMailSender.send(message);
    }

    public AdminUser saveUser(RegistrationForm form) {
        String email = form.getEmail();
        String userName = form.getName();
        String password = encodePassword(form.getPassword());

        // This is security no-no but shortcutting since it's just a blog
        // basically first user added is auto approved, everyone else has
        // to go through the steps
        AdminUser newUser = new AdminUser(
            email,
            userName,
            password
        );

        if (!adminUserRepository.existsBy()) {
            newUser.setActivated(true);
        }

        adminUserRepository.save(newUser);
        return newUser;
    }

    public boolean activateUser(String name) {
        Optional<AdminUser> user = adminUserRepository.findByUsername(name);

        if (user.isEmpty()) {
            return false;
        }

        AdminUser unwrapped = user.get();
        unwrapped.setActivated(true);
        adminUserRepository.save(unwrapped);
        return true;
    }

    /**
     * Deletes unapproved users whose approval link has expired, i.e. created
     * more than {@link JsonWebTokenUtils#MAX_TIME_VERIFY} before {@code now}.
     * Activated users are never touched.
     *
     * @return how many users were deleted
     */
    @Transactional
    public int deleteExpiredInactiveUsers(Instant now) {
        Instant cutoff = now.minusMillis(JsonWebTokenUtils.MAX_TIME_VERIFY);
        return adminUserRepository.deleteInactiveCreatedBefore(cutoff);
    }

    private static String encodePassword(String password) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }
}
