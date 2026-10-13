package com.portfolio.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Security model
 * <ul>
 *   <li>Public: the static site, GET /api/** read endpoints, POST /api/contact, health probe.</li>
 *   <li>Admin: /api/admin/** requires ROLE_ADMIN, obtained via session login at /api/auth/login.</li>
 *   <li>CSRF protection stays ON: a readable XSRF-TOKEN cookie is echoed in the X-XSRF-TOKEN header.</li>
 *   <li>Hardened response headers incl. a strict Content-Security-Policy (no inline scripts/styles).</li>
 *   <li>Admin password is BCrypt-hashed and supplied from the environment; nothing is hardcoded.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AdminProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(c -> c.sameSite("Strict").path("/"));

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .addFilterBefore(new RateLimitFilter(), CsrfFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**", "/api/auth/me").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/contact", "/api/auth/login").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/*.html", "/css/**", "/js/**", "/img/**",
                        "/favicon.svg", "/robots.txt", "/api/profile", "/api/skills", "/api/projects",
                        "/api/testimonials").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/", "/*.html").permitAll()
                .anyRequest().denyAll())
            // JSON friendly session login: 200 on success, 401 on failure (no redirects).
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value()))
                .failureHandler((req, res, ex) -> res.setStatus(HttpStatus.UNAUTHORIZED.value())))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value())))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(String.join("; ",
                        "default-src 'self'",
                        "script-src 'self'",
                        "style-src 'self'",
                        "img-src 'self' data:",
                        "font-src 'self'",
                        "connect-src 'self'",
                        "object-src 'none'",
                        "base-uri 'self'",
                        "form-action 'self'",
                        "frame-ancestors 'none'")))
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()")))
            .httpBasic(basic -> basic.disable()); // same-origin only: no CORS configuration is registered

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Single admin account from configuration. When no credential is configured an
     * empty store is used, so the admin API is simply unreachable (fail closed).
     */
    @Bean
    UserDetailsService userDetailsService(AdminProperties props, PasswordEncoder encoder) {
        if (!props.isConfigured()) {
            log.warn("No admin credential configured (ADMIN_PASSWORD_HASH / ADMIN_PASSWORD): admin API disabled");
            return new InMemoryUserDetailsManager(List.of());
        }
        String hash = props.passwordHash();
        if (hash == null || hash.isBlank()) {
            if (props.password().length() < AdminProperties.MIN_PASSWORD_LENGTH) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD must be at least " + AdminProperties.MIN_PASSWORD_LENGTH + " characters");
            }
            hash = encoder.encode(props.password());
        }
        UserDetails admin = User.withUsername(props.username()).password(hash).roles("ADMIN").build();
        return new InMemoryUserDetailsManager(admin);
    }
}
