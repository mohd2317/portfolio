package com.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Admin credentials, bound from {@code portfolio.admin.*}. Nothing is hardcoded:
 * supply either a BCrypt hash ({@code ADMIN_PASSWORD_HASH}, preferred) or a plain
 * password ({@code ADMIN_PASSWORD}, hashed in memory at startup) via environment
 * variables or a secret store. If neither is set the admin area stays disabled.
 */
@ConfigurationProperties("portfolio.admin")
public record AdminProperties(String username, String password, String passwordHash) {

    public static final int MIN_PASSWORD_LENGTH = 12;

    public AdminProperties {
        username = StringUtils.hasText(username) ? username : "admin";
    }

    /** True when a credential has been supplied. */
    public boolean isConfigured() {
        return StringUtils.hasText(password) || StringUtils.hasText(passwordHash);
    }
}
