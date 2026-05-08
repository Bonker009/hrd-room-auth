package org.kshrd.hrdroomservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.classroom.AssessmentIdsRequest;
import org.kshrd.hrdroomservice.api.dto.classroom.AssessmentSummaryResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomRequest;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomStudentCountResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.StudentCurrentClassroomResponse;
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
public class ClassroomController {

    private final ClassroomService classroomService;

    @GetMapping("/my-classrooms")
    @Operation(
            summary = "List classrooms I teach (paged)",
            description =
                    "Returns the classrooms assigned to the authenticated teacher, ordered by"
                            + " creation date.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<PageResponse<ClassroomResponse>>> myClassrooms(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        UUID teacherId =
                SecurityUtils.currentUserId()
                        .orElseThrow(() -> new AccessDeniedException("Missing JWT subject"));
        return ResponseUtil.ok(classroomService.myClassrooms(teacherId, page, size), "OK");
    }

    @GetMapping("/my-classroom")
    @Operation(
            summary = "Get my current classroom",
            description =
                    "Returns the authenticated student's classroom for the active academic year,"
                            + " the year's generation, and the courses they are currently enrolled"
                            + " in.")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<ApiResponse<StudentCurrentClassroomResponse>> myCurrentClassroom() {
        UUID studentId =
                SecurityUtils.currentUserId()
                        .orElseThrow(() -> new AccessDeniedException("Missing JWT subject"));
        return ResponseUtil.ok(classroomService.currentForStudent(studentId), "OK");
    }

    @GetMapping("/with-teachers")
    @Operation(
            summary = "List classrooms with their teacher IDs",
            description = "Returns every classroom along with the IDs of the teachers assigned.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> withTeachers() {
        return ResponseUtil.ok(classroomService.listWithTeachers(), "OK");
    }

    @GetMapping("/with-subjects")
    @Operation(
            summary = "List classrooms with their subject IDs",
            description = "Returns every classroom along with the IDs of the subjects taught.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> withSubjects() {
        return ResponseUtil.ok(classroomService.listWithSubjects(), "OK");
    }

    @GetMapping("/with-students")
    @Operation(
            summary = "List classrooms with their student IDs",
            description = "Returns every classroom along with the IDs of the students enrolled.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> withStudents() {
        return ResponseUtil.ok(classroomService.listWithStudents(), "OK");
    }

    @GetMapping("/student-counts")
    @Operation(
            summary = "Count students per classroom",
            description = "Returns one row per classroom with the total number of enrolled students.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<List<ClassroomStudentCountResponse>>> studentCounts() {
        return ResponseUtil.ok(classroomService.studentCounts(), "OK");
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(
            summary = "List classrooms taught for a subject",
            description = "Returns the classrooms that include the given subject in their schedule.")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> bySubject(
            @PathVariable UUID subjectId) {
        return ResponseUtil.ok(classroomService.bySubject(subjectId), "OK");
    }

    @GetMapping
    @Operation(
            summary = "List all classrooms (paged)",
            description = "Returns every classroom in the system, newest first.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<PageResponse<ClassroomResponse>>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseUtil.ok(classroomService.listAll(page, size), "OK");
    }

    @PostMapping
    @Operation(
            summary = "Create a classroom",
            description = "Creates a new classroom under the active academic year.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> create(
            @Valid @RequestBody ClassroomRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.created(
                classroomService.create(request, actor), "Classroom created successfully");
    }

    @PutMapping("/{classroomId}")
    @Operation(
            summary = "Update a classroom",
            description = "Updates an existing classroom's name, abbreviation, description or image.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> update(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseUtil.ok(
                classroomService.update(classroomId, request, actor), "Classroom updated");
    }

    @DeleteMapping("/{classroomId}")
    @Operation(
            summary = "Delete a classroom",
            description = "Removes a classroom along with its teacher, student, subject and"
                    + " assessment links.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID classroomId) {
        classroomService.delete(classroomId);
        return ResponseUtil.noContent("Classroom deleted");
    }

    @PostMapping(
            value = "/{classroomId}/upload-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a classroom cover image",
            description = "Uploads a JPEG/PNG/GIF/WebP image (max 20MB) and stores its public URL.")
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
    @Operation(
            summary = "List assessments assigned to a classroom",
            description = "Returns the assessment summaries linked to the given classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<AssessmentSummaryResponse>>> assessments(
            @PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.listAssessments(classroomId), "OK");
    }

    @PostMapping("/{classroomId}/assign-assessments")
    @Operation(
            summary = "Assign assessments to a classroom",
            description = "Links one or more assessments to the given classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> assignAssessments(
            @PathVariable UUID classroomId, @Valid @RequestBody AssessmentIdsRequest request) {
        classroomService.assignAssessments(classroomId, request);
        return ResponseUtil.ok(null, "Assessments assigned");
    }

    @PostMapping("/{classroomId}/remove-assessments")
    @Operation(
            summary = "Remove assessments from a classroom",
            description = "Unlinks the given assessments from the classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> removeAssessments(
            @PathVariable UUID classroomId, @Valid @RequestBody AssessmentIdsRequest request) {
        classroomService.removeAssessments(classroomId, request);
        return ResponseUtil.ok(null, "Assessments removed");
    }

    @PostMapping("/{classroomId}/assign-teachers")
    @Operation(
            summary = "Assign teachers to a classroom",
            description = "Links one or more teachers to the given classroom.")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Void>> assignTeachers(
            @PathVariable UUID classroomId, @Valid @RequestBody UuidListRequest request) {
        classroomService.assignTeachers(classroomId, request);
        return ResponseUtil.ok(null, "Teachers assigned");
    }

    @PostMapping("/{classroomId}/remove-teachers")
    @Operation(
            summary = "Remove teachers from a classroom",
            description = "Unlinks the given teachers from the classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> removeTeachers(
            @PathVariable UUID classroomId, @Valid @RequestBody UuidListRequest request) {
        classroomService.removeTeachers(classroomId, request);
        return ResponseUtil.ok(null, "Teachers removed");
    }

    @GetMapping("/{classroomId}/teachers")
    @Operation(
            summary = "List teachers in a classroom",
            description = "Returns the IDs of all teachers assigned to the classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<UUID>>> teachers(@PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.listTeacherIds(classroomId), "OK");
    }

    @PostMapping("/{classroomId}/assign-subject")
    @Operation(
            summary = "Assign a subject to a classroom",
            description = "Links one subject to the given classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> assignSubject(
            @PathVariable UUID classroomId, @RequestParam UUID subjectId) {
        classroomService.assignSubject(classroomId, subjectId);
        return ResponseUtil.ok(null, "Subject assigned");
    }

    @DeleteMapping("/{classroomId}/remove-subject")
    @Operation(
            summary = "Remove a subject from a classroom",
            description = "Unlinks the given subject from the classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> removeSubject(
            @PathVariable UUID classroomId, @RequestParam UUID subjectId) {
        classroomService.removeSubject(classroomId, subjectId);
        return ResponseUtil.ok(null, "Subject removed");
    }

    @GetMapping("/{classroomId}/subjects")
    @Operation(
            summary = "List subjects in a classroom",
            description = "Returns the IDs of all subjects taught in the classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<UUID>>> subjects(@PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.listSubjectIds(classroomId), "OK");
    }

    @PostMapping("/{classroomId}/assign-students")
    @Operation(
            summary = "Assign students to a classroom",
            description = "Links one or more students to the given classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> assignStudents(
            @PathVariable UUID classroomId, @Valid @RequestBody UuidListRequest request) {
        classroomService.assignStudents(classroomId, request);
        return ResponseUtil.ok(null, "Students assigned");
    }

    @PostMapping("/{classroomId}/remove-students")
    @Operation(
            summary = "Remove students from a classroom",
            description = "Unlinks the given students from the classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> removeStudents(
            @PathVariable UUID classroomId, @Valid @RequestBody UuidListRequest request) {
        classroomService.removeStudents(classroomId, request);
        return ResponseUtil.ok(null, "Students removed");
    }

    @GetMapping("/{classroomId}/students")
    @Operation(
            summary = "List students in a classroom",
            description = "Returns the IDs of all students currently enrolled in the classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<List<UUID>>> students(@PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.listStudentIds(classroomId), "OK");
    }

    @PostMapping("/{sourceClassroomId}/move-student")
    @Operation(
            summary = "Move a student between classrooms",
            description = "Moves a student to another classroom within the same active academic"
                    + " year.")
    @PreAuthorize("hasAnyRole('admin','teacher')")
    public ResponseEntity<ApiResponse<Void>> moveStudent(
            @PathVariable UUID sourceClassroomId,
            @RequestParam UUID targetClassroomId,
            @RequestParam UUID studentId) {
        classroomService.moveStudent(sourceClassroomId, targetClassroomId, studentId);
        return ResponseUtil.ok(null, "Student moved");
    }

    @GetMapping("/{classroomId}")
    @Operation(
            summary = "Get a classroom by ID",
            description = "Returns the basic details of a single classroom.")
    @PreAuthorize("hasAnyRole('admin','teacher','student')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> byId(@PathVariable UUID classroomId) {
        return ResponseUtil.ok(classroomService.getById(classroomId), "OK");
    }
}
