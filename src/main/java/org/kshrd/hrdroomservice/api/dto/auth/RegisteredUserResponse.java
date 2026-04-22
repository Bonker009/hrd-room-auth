package org.kshrd.hrdroomservice.api.dto.auth;

import lombok.Builder;

@Builder
public record RegisteredUserResponse(
        String userId, String username, String email, String firstName, String lastName) {}
