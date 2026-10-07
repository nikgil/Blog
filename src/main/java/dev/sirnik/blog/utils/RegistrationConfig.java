package dev.sirnik.blog.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.registration")
public record RegistrationConfig(
    boolean enabled,
    Mail mail,
    String url,
    String jwt
) {
    public record Mail(
        String host,
        int port,
        String username,
        String password,
        String approver,
        String sender
    ) {
    }
}
