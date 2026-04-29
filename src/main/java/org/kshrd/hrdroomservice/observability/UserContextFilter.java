package org.kshrd.hrdroomservice.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.kshrd.hrdroomservice.security.SecurityUtils;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        SecurityUtils.currentUserId().ifPresent(userId -> MDC.put("userId", userId.toString()));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }
}
