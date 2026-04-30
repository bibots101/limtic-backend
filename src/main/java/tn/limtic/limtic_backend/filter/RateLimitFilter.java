package tn.limtic.limtic_backend.filter;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    // 1 bucket par IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket getBucket(String ip) {
        return buckets.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(
                    10,                                    // 10 tentatives
                    Refill.greedy(10, Duration.ofMinutes(1)) // rechargé toutes les minutes
                ))
                .build()
        );
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        // Appliqué uniquement sur les routes d'authentification
        if (path.startsWith("/api/auth/")) {
            String ip     = getClientIp(request);
            Bucket bucket = getBucket(ip);

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"error\":\"Trop de tentatives. Réessayez dans une minute.\"}"
                );
                return;
            }
        }

        chain.doFilter(req, res);
    }

    // Gère le cas reverse proxy (X-Forwarded-For)
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}