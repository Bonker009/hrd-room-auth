package org.kshrd.hrdroomservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.kshrd.hrdroomservice.api.dto.auth.AuthMeResponse;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v4/account")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthMeResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseUtil.ok(
                AuthMeResponse.builder()
                        .subject(jwt.getSubject())
                        .username(jwt.getClaimAsString("preferred_username"))
                        .email(jwt.getClaimAsString("email"))
                        .firstName(jwt.getClaimAsString("given_name"))
                        .lastName(jwt.getClaimAsString("family_name"))
                        .roles(extractRealmRoles(jwt))
                        .build(),
                "Current user profile");
    }

    private static List<String> extractRealmRoles(Jwt jwt) {
        Object claim = jwt.getClaim("realm_access");
        if (!(claim instanceof Map<?, ?> realm)) {
            return List.of();
        }
        Object rolesObj = realm.get("roles");
        if (!(rolesObj instanceof List<?> rawRoles)) {
            return List.of();
        }
        return rawRoles.stream().filter(r -> r != null).map(Object::toString).toList();
    }
}
