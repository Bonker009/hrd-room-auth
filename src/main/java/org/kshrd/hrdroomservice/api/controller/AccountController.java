package org.kshrd.hrdroomservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.auth.AuthMeResponse;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.kshrd.hrdroomservice.service.account.ActiveAcademicContext;
import org.kshrd.hrdroomservice.service.account.ActiveAcademicContextService;
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
@RequiredArgsConstructor
public class AccountController {

    private final ActiveAcademicContextService activeAcademicContextService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthMeResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        UUID studentId = parseStudentId(jwt.getSubject());
        Optional<ActiveAcademicContext> ctx =
                studentId == null ? Optional.empty() : activeAcademicContextService.resolveForStudent(studentId);
        AuthMeResponse body =
                new AuthMeResponse(
                        jwt.getSubject(),
                        jwt.getClaimAsString("preferred_username"),
                        jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("given_name"),
                        jwt.getClaimAsString("family_name"),
                        extractRealmRoles(jwt),
                        ctx.map(ActiveAcademicContext::academicYearId).orElse(null),
                        ctx.map(ActiveAcademicContext::academicYearName).orElse(null),
                        ctx.map(ActiveAcademicContext::generation).orElse(null));
        return ResponseUtil.ok(body, "Current user profile");
    }

    private static UUID parseStudentId(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
