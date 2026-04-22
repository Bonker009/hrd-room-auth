package org.kshrd.hrdroomservice.api.dto.auth;

import java.util.List;
import lombok.Builder;

@Builder
public record AuthMeResponse(
        String subject, String username, String email, String firstName, String lastName, List<String> roles) {}
