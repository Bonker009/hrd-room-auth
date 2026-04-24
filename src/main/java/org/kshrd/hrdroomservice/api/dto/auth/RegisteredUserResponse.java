package org.kshrd.hrdroomservice.api.dto.auth;

public record RegisteredUserResponse(
        String userId, String username, String email, String firstName, String lastName) {}
