package org.kshrd.hrdroomservice.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class Authz {

    private Authz() {}

    public static RequestPostProcessor admin() {
        return admin(UUID.randomUUID());
    }

    public static RequestPostProcessor admin(UUID userId) {
        return as(userId, "admin");
    }

    public static RequestPostProcessor teacher(UUID userId) {
        return as(userId, "teacher");
    }

    public static RequestPostProcessor student(UUID userId) {
        return as(userId, "student");
    }

    private static RequestPostProcessor as(UUID userId, String role) {
        return jwt().jwt(t -> t.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
