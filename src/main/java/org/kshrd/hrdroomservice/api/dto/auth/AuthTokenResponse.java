package org.kshrd.hrdroomservice.api.dto.auth;

import lombok.Builder;

@Builder
public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        long refreshExpiresIn,
        String tokenType,
        String scope) {}
