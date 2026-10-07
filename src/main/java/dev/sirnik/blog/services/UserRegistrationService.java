package dev.sirnik.blog.services;

import org.springframework.stereotype.Service;

import dev.sirnik.blog.models.forms.RegistrationForm;
import dev.sirnik.blog.repositories.AdminUserRepository;

@Service
public class UserRegistrationService {

    private final AdminUserRepository adminUserRepository;

    public UserRegistrationService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
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

        // min and max length
        if (form.getPassword().length() > 70
            || form.getPassword().length() < 3) {
            return false;
        }

        return true;
    }
}
