package org.kshrd.hrdroomservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.classroom.AssessmentIdsRequest;
import org.kshrd.hrdroomservice.api.dto.classroom.AssessmentSummaryResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomRequest;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomStudentCountResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.UuidListRequest;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.kshrd.hrdroomservice.security.SecurityUtils;
import org.kshrd.hrdroomservice.service.classroom.ClassroomService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v4/classrooms")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "02. Classroom", description = "Classrooms")
public class ClassroomController {

    private final ClassroomService classroomService;

    @GetMapping("/my-classrooms")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<PageResponse<ClassroomResponse>>> myClassrooms(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        UUID teacherId =
                SecurityUtils.currentUserId()
                        .orElseThrow(() -> new AccessDeniedException("Missing JWT subject"));
        return ResponseUtil.ok(classroomService.myClassrooms(teacherId, page, size), "OK");
    }

    @GetMapping("/with-teachers")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> withTeachers() {
        return ResponseUtil.ok(classroomService.listWithTeachers(), "OK");
    }

    @GetMapping("/with-subjects")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> withSubjects() {
        return ResponseUtil.ok(classroomService.listWithSubjects(), "OK");
    }

    @GetMapping("/with-students")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> withStudents() {
        return ResponseUtil.ok(classroomService.listWithStudents(), "OK");
    }

    @GetMapping("/student-counts")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<ClassroomStudentCountResponse>>> studentCounts() {
        return ResponseUtil.ok(classroomService.studentCounts(), "OK");
    }

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> bySubject(@PathVariable UUID subjectId) {
        return ResponseUtil.ok(classroomService.bySubject(subjectId), "OK");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<PageResponse<ClassroomResponse>>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseUtil.ok(classroomService.listAll(page, size), "OK");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> create(
            @Valid @RequestBody ClassroomRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.created(classroomService.create(request, actor), "Classroom created successfully");
    }

    @PutMapping("/{classroomId}")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> update(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.ok(classroomService.update(classroomId, request, actor), "Classroom updated");
    }

    @DeleteMapping("/{classroomId}")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID classroomId) {
        classroomService.delete(classroomId);
        return ResponseUtil.noContent("Classroom deleted");
    }

    @PostMapping(value = "/{classroomId}/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @PathVariable UUID classroomId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        String url = classroomService.uploadImage(classroomId, file, actor);
        return ResponseUtil.ok(Map.of("url", url), "Image uploaded");
    }

    @GetMapping("/{classroomId}/assessments")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<AssessmentSummaryResponse>>> assessments(
            @PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.listAssessments(classroomId), "OK");
    }

    @PostMapping("/{classroomId}/assign-assessments")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> assignAssessments(
            @PathVariable UUID classroomId, @Valid @RequestBody AssessmentIdsRequest request) {
        classroomService.assignAssessments(classroomId, request);
        return ResponseUtil.ok(null, "Assessments assigned");
    }

    @PostMapping("/{classroomId}/remove-assessments")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> removeAssessments(
            @PathVariable UUID classroomId, @Valid @RequestBody AssessmentIdsRequest request) {
        classroomService.removeAssessments(classroomId, request);
        return ResponseUtil.ok(null, "Assessments removed");
    }

    @PostMapping("/{classroomId}/assign-teachers")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Void>> assignTeachers(
            @PathVariable UUID classroomId, @Valid @RequestBody UuidListRequest request) {
        classroomService.assignTeachers(classroomId, request);
        return ResponseUtil.ok(null, "Teachers assigned");
    }

    @PostMapping("/{classroomId}/remove-teachers")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> removeTeachers(
            @PathVariable UUID classroomId, @Valid @RequestBody UuidListRequest request) {
        classroomService.removeTeachers(classroomId, request);
        return ResponseUtil.ok(null, "Teachers removed");
    }

    @GetMapping("/{classroomId}/teachers")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<UUID>>> teachers(@PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.listTeacherIds(classroomId), "OK");
    }

    @PostMapping("/{classroomId}/assign-subject")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> assignSubject(
            @PathVariable UUID classroomId, @RequestParam UUID subjectId) {
        classroomService.assignSubject(classroomId, subjectId);
        return ResponseUtil.ok(null, "Subject assigned");
    }

    @DeleteMapping("/{classroomId}/remove-subject")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> removeSubject(
            @PathVariable UUID classroomId, @RequestParam UUID subjectId) {
        classroomService.removeSubject(classroomId, subjectId);
        return ResponseUtil.ok(null, "Subject removed");
    }

    @GetMapping("/{classroomId}/subjects")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<UUID>>> subjects(@PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.listSubjectIds(classroomId), "OK");
    }

    @PostMapping("/{classroomId}/assign-students")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> assignStudents(
            @PathVariable UUID classroomId, @Valid @RequestBody UuidListRequest request) {
        classroomService.assignStudents(classroomId, request);
        return ResponseUtil.ok(null, "Students assigned");
    }

    @PostMapping("/{classroomId}/remove-students")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> removeStudents(
            @PathVariable UUID classroomId, @Valid @RequestBody UuidListRequest request) {
        classroomService.removeStudents(classroomId, request);
        return ResponseUtil.ok(null, "Students removed");
    }

    @GetMapping("/{classroomId}/students")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<UUID>>> students(@PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.listStudentIds(classroomId), "OK");
    }

    @PostMapping("/{sourceClassroomId}/move-student")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> moveStudent(
            @PathVariable UUID sourceClassroomId,
            @RequestParam UUID targetClassroomId,
            @RequestParam UUID studentId) {
        classroomService.moveStudent(sourceClassroomId, targetClassroomId, studentId);
        return ResponseUtil.ok(null, "Student moved");
    }

    @GetMapping("/{classroomId}")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> byId(@PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.getById(classroomId), "OK");
    }
}
