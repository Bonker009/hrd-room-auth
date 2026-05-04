package org.kshrd.hrdroomservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.course.CourseResponse;
import org.kshrd.hrdroomservice.api.dto.course.CourseUpdateRequest;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.kshrd.hrdroomservice.security.SecurityUtils;
import org.kshrd.hrdroomservice.service.course.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v4/courses")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final CourseService courseService;

    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.ok(courseService.update(courseId, request, actor), "Course updated");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        if (includeArchived && !SecurityUtils.isAdmin()) {
            return ResponseUtil.forbidden("includeArchived is restricted to admin");
        }
        boolean onlyActiveYears = !(SecurityUtils.isAdmin() && includeArchived);
        return ResponseUtil.ok(
                courseService.list(
                        type, academicYearId, includeArchived, onlyActiveYears, page, size),
                "OK");
    }

    @GetMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<CourseResponse>> byId(@PathVariable UUID courseId) {
        return ResponseUtil.ok(courseService.getById(courseId), "OK");
    }
}
