package com.portfolio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security loads the CSRF token lazily, so the XSRF-TOKEN cookie would
 * never be written for a single-page client. Touching the token here forces the
 * cookie to be sent so JavaScript can echo it back in the X-XSRF-TOKEN header.
 */
class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken(); // renders the deferred token => cookie is written
        }
        chain.doFilter(request, response);
    }
}
