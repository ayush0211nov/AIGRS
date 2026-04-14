package com.aigrs.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiting filter using Redis sliding window counter.
 * Limits requests per IP to a configurable rate (default 100/min).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.requests-per-minute:100}")
    private int maxRequestsPerMinute;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIp(httpRequest);
        String key = "rate:" + clientIp;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            if (currentCount != null && currentCount > maxRequestsPerMinute) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(
                        "{\"status\":\"error\",\"message\":\"Rate limit exceeded. Try again later.\",\"error_code\":\"RATE_LIMIT_EXCEEDED\"}"
                );
                return;
            }
        } catch (Exception e) {
            // If Redis is down, allow the request through rather than blocking
            log.warn("Rate limiting failed (Redis issue): {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
