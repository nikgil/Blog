package dev.sirnik.blog.services;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Spring Boot calls every ApplicationRunner bean once, after the context has
 * started. With the server restarted daily, this is the sweep that removes
 * registrations nobody approved in time; there is no scheduler.
 */
@Component
public class ExpiredUserCleanupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory
        .getLogger(ExpiredUserCleanupRunner.class);

    private final UserRegistrationService registrationService;

    public ExpiredUserCleanupRunner(
        UserRegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int removed = registrationService
            .deleteExpiredInactiveUsers(Instant.now());

        log.info("Removed {} expired unapproved registration(s)", removed);
    }
}
