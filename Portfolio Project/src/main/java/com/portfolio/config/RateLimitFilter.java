package com.portfolio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Minimal in-memory sliding-window limiter protecting the two abuse-prone
 * endpoints: the public contact form and the admin login (brute force).
 * It is per client address; for multi-node deployments swap for a shared store
 * (Redis / API gateway). Behind a proxy enable {@code server.forward-headers-strategy}
 * so {@code getRemoteAddr()} reflects the real client.
 */
class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    /** A rule: at most {@code limit} requests per {@code window} for a path. */
    private record Rule(String path, int limit, Duration window) { }

    private final Rule[] rules = {
            new Rule("/api/contact", 5, Duration.ofMinutes(10)),
            new Rule("/api/auth/login", 10, Duration.ofMinutes(1))
    };

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Rule rule = HttpMethod.POST.matches(request.getMethod()) ? ruleFor(request.getRequestURI()) : null;
        if (rule != null && !allow(rule, request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(rule.window().toSeconds()));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(
                    "{\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"Please try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private Rule ruleFor(String uri) {
        for (Rule r : rules) {
            if (r.path().equals(uri)) {
                return r;
            }
        }
        return null;
    }

    private boolean allow(Rule rule, String client) {
        long now = System.currentTimeMillis();
        long windowStart = now - rule.window().toMillis();
        if (hits.size() > MAX_TRACKED_CLIENTS) { // bound memory: drop idle entries
            hits.values().removeIf(q -> q.isEmpty() || q.peekLast() < windowStart);
        }
        Deque<Long> queue = hits.computeIfAbsent(rule.path() + '|' + client, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() < windowStart) {
                queue.pollFirst();
            }
            if (queue.size() >= rule.limit()) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }
}
