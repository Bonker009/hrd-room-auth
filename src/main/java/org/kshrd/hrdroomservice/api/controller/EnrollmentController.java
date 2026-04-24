package org.kshrd.hrdroomservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentByCourseTypeResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.TerminateEnrollmentRequest;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.kshrd.hrdroomservice.security.SecurityUtils;
import org.kshrd.hrdroomservice.service.enrollment.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
@RequestMapping("/api/v4/enrollments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @RequestParam UUID studentId,
            @RequestParam UUID courseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.created(enrollmentService.enroll(studentId, courseId, actor), "Enrolled");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> listAll(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseUtil.ok(enrollmentService.listAll(includeArchived), "OK");
    }

    @GetMapping("/by-course-type")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<EnrollmentByCourseTypeResponse>> byCourseType(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseUtil.ok(enrollmentService.listByCourseType(includeArchived), "OK");
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> byStudent(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        if (SecurityUtils.hasRole("student")) {
            UUID self =
                    SecurityUtils.currentUserId()
                            .orElseThrow(() -> new AccessDeniedException("Missing subject"));
            if (!self.equals(studentId)) {
                return ResponseUtil.forbidden("Students may only query their own enrollments");
            }
            if (includeArchived) {
                return ResponseUtil.forbidden("Students cannot include archived enrollments");
            }
        }
        return ResponseUtil.ok(enrollmentService.listByStudent(studentId, includeArchived), "OK");
    }

    @GetMapping("/student/{studentId}/by-course-type")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<EnrollmentByCourseTypeResponse>> byStudentGrouped(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        if (SecurityUtils.hasRole("student")) {
            UUID self =
                    SecurityUtils.currentUserId()
                            .orElseThrow(() -> new AccessDeniedException("Missing subject"));
            if (!self.equals(studentId)) {
                return ResponseUtil.forbidden("Students may only query their own enrollments");
            }
            if (includeArchived) {
                return ResponseUtil.forbidden("Students cannot include archived enrollments");
            }
        }
        return ResponseUtil.ok(enrollmentService.listByStudentGrouped(studentId, includeArchived), "OK");
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> byCourse(@PathVariable UUID courseId) {
        return ResponseUtil.ok(enrollmentService.listByCourse(courseId), "OK");
    }

    @GetMapping("/{enrollmentId}")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> byId(@PathVariable UUID enrollmentId) {
        EnrollmentResponse row = enrollmentService.getById(enrollmentId);
        if (SecurityUtils.hasRole("student")) {
            UUID self =
                    SecurityUtils.currentUserId()
                            .orElseThrow(() -> new AccessDeniedException("Missing subject"));
            if (!self.equals(row.studentId())) {
                return ResponseUtil.forbidden("Students may only load their own enrollment");
            }
        }
        return ResponseUtil.ok(row, "OK");
    }

    @PutMapping("/{enrollmentId}/terminate")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> terminate(
            @PathVariable UUID enrollmentId,
            @RequestBody(required = false) TerminateEnrollmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.ok(
                enrollmentService.terminate(enrollmentId, request, actor), "Enrollment terminated");
    }

    @PutMapping("/{enrollmentId}/reactivate")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> reactivate(
            @PathVariable UUID enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.ok(enrollmentService.reactivate(enrollmentId, actor), "Enrollment reactivated");
    }

    @PostMapping("/move-to-advanced")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> moveToAdvanced(
            @RequestParam UUID studentId,
            @RequestParam UUID basicCourseId,
            @RequestParam UUID advancedCourseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.created(
                enrollmentService.moveToAdvanced(studentId, basicCourseId, advancedCourseId, actor),
                "Moved to advanced");
    }
}
