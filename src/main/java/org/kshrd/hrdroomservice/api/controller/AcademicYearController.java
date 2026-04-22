package org.kshrd.hrdroomservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearRequest;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearResponse;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.kshrd.hrdroomservice.service.academicyear.AcademicYearService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v4/academic-years")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> create(
            @Valid @RequestBody AcademicYearRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        AcademicYearResponse created = academicYearService.create(request, actor);
        return ResponseUtil.created(created, "Academic year created successfully");
    }

    @PutMapping("/{academicYearId}/activate")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> activate(
            @PathVariable UUID academicYearId, @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.ok(
                academicYearService.activate(academicYearId, actor), "Academic year activated");
    }

    @PutMapping("/{academicYearId}/archive")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> archive(
            @PathVariable UUID academicYearId, @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.ok(
                academicYearService.archive(academicYearId, actor), "Academic year archived");
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> active() {
        return academicYearService
                .findActive()
                .map(y -> ResponseUtil.ok(y, "Active academic year"))
                .orElseGet(() -> ResponseUtil.notFound("No active academic year"));
    }

    @GetMapping("/{academicYearId}")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> byId(@PathVariable UUID academicYearId) {
        return ResponseUtil.ok(academicYearService.getById(academicYearId), "OK");
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> list(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseUtil.ok(academicYearService.list(includeArchived), "OK");
    }
}
