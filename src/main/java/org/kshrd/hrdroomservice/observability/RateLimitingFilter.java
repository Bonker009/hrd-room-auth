package org.kshrd.hrdroomservice.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.config.RateLimitProperties;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets;

    public RateLimitingFilter(ObjectMapper objectMapper, RateLimitProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.buckets =
                Caffeine.newBuilder()
                        .maximumSize(20_000)
                        .expireAfterAccess(Duration.ofHours(1))
                        .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!shouldRateLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = ClientIpResolver.resolve(request);
        Bucket bucket = buckets.get(clientKey, k -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(
                "Retry-After", String.valueOf(Math.max(1, properties.getRefillSeconds())));
        ApiResponse<Void> body =
                ApiResponse.error(
                        429, "Too many requests", "RATE_LIMIT_EXCEEDED", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private Bucket newBucket() {
        long capacity = Math.max(1, properties.getCapacity());
        int seconds = Math.max(1, properties.getRefillSeconds());
        return Bucket.builder()
                .addLimit(
                        b ->
                                b.capacity(capacity)
                                        .refillIntervally(capacity, Duration.ofSeconds(seconds)))
                .build();
    }

    private static boolean shouldRateLimit(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/");
    }
}
