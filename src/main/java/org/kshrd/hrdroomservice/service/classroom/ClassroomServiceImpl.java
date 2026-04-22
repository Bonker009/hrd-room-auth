package org.kshrd.hrdroomservice.service.classroom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.AssessmentEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomStudentRow;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomSubjectRow;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomTeacherRow;
import org.kshrd.hrdroomservice.persistence.mapper.AcademicYearMapper;
import org.kshrd.hrdroomservice.persistence.mapper.ClassroomMapper;
import org.kshrd.hrdroomservice.persistence.mapper.ClassroomRelationMapper;
import org.kshrd.hrdroomservice.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;

    private final ClassroomMapper classroomMapper;
    private final ClassroomRelationMapper relationMapper;
    private final AcademicYearMapper academicYearMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ClassroomResponse create(ClassroomRequest request, UUID actorId) {
        AcademicYearEntity active = academicYearMapper.findActive();
        if (active == null) {
            throw ApiException.badRequest("No active academic year");
        }
        ClassroomEntity row = new ClassroomEntity();
        row.setClassroomId(UUID.randomUUID());
        row.setClassName(request.getClassName().trim());
        row.setClassroomAbbre(request.getClassroomAbbre().trim());
        row.setDescription(request.getDescription());
        row.setImage(request.getImage());
        row.setAcademicYearId(active.getAcademicYearId());
        row.setCreatedBy(actorId);
        row.setUpdatedBy(actorId);
        row.setCreatedAt(LocalDateTime.now());
        classroomMapper.insert(row);
        if (request.getSubjectId() != null) {
            relationMapper.insertSubject(row.getClassroomId(), request.getSubjectId());
        }
        return toBasic(classroomMapper.findById(row.getClassroomId()));
    }

    @Override
    @Transactional
    public ClassroomResponse update(UUID classroomId, ClassroomRequest request, UUID actorId) {
        ClassroomEntity existing = classroomMapper.findById(classroomId);
        if (existing == null) {
            throw ApiException.notFound("Classroom not found");
        }
        existing.setClassName(request.getClassName().trim());
        existing.setClassroomAbbre(request.getClassroomAbbre().trim());
        existing.setDescription(request.getDescription());
        if (StringUtils.hasText(request.getImage())) {
            existing.setImage(request.getImage());
        }
        existing.setUpdatedBy(actorId);
        classroomMapper.update(existing);
        return toBasic(classroomMapper.findById(classroomId));
    }

    @Override
    @Transactional
    public void delete(UUID classroomId) {
        ClassroomEntity existing = classroomMapper.findById(classroomId);
        if (existing == null) {
            throw ApiException.notFound("Classroom not found");
        }
        relationMapper.deleteAllAssessments(classroomId);
        relationMapper.deleteAllStudents(classroomId);
        relationMapper.deleteAllSubjects(classroomId);
        relationMapper.deleteAllTeachers(classroomId);
        classroomMapper.delete(classroomId);
    }

    @Override
    @Transactional
    public String uploadImage(UUID classroomId, MultipartFile file, UUID actorId) {
        ClassroomEntity existing = classroomMapper.findById(classroomId);
        if (existing == null) {
            throw ApiException.notFound("Classroom not found");
        }
        validateImage(file);
        String url = fileStorageService.uploadFile(file, "classrooms");
        classroomMapper.updateImage(classroomId, url, actorId);
        return url;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("File is required");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw ApiException.badRequest("File must be 20 MB or smaller");
        }
        String ct = file.getContentType();
        if (ct == null
                || !(ct.equals("image/jpeg")
                        || ct.equals("image/png")
                        || ct.equals("image/gif")
                        || ct.equals("image/webp"))) {
            throw ApiException.badRequest("Unsupported image content type");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomResponse getById(UUID classroomId) {
        ClassroomEntity row = classroomMapper.findById(classroomId);
        if (row == null) {
            throw ApiException.notFound("Classroom not found");
        }
        return toBasic(row);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentSummaryResponse> listAssessments(UUID classroomId) {
        if (classroomMapper.findById(classroomId) == null) {
            throw ApiException.notFound("Classroom not found");
        }
        return relationMapper.listAssessments(classroomId).stream()
                .map(this::toAssessmentSummary)
                .toList();
    }

    private AssessmentSummaryResponse toAssessmentSummary(AssessmentEntity a) {
        return AssessmentSummaryResponse.builder()
                .assessmentId(a.getAssessmentId())
                .name(a.getName())
                .assessmentDate(a.getAssessmentDate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassroomResponse> myClassrooms(UUID teacherId, int page, int size) {
        int offset = page * size;
        long total = classroomMapper.countForTeacher(teacherId);
        List<ClassroomResponse> content =
                classroomMapper.pageForTeacher(teacherId, offset, size).stream().map(this::toBasic).toList();
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassroomResponse> listAll(int page, int size) {
        int offset = page * size;
        long total = classroomMapper.countAll();
        List<ClassroomResponse> content =
                classroomMapper.pageAll(offset, size).stream().map(this::toBasic).toList();
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public void assignAssessments(UUID classroomId, AssessmentIdsRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.getAssessmentIds()) {
            relationMapper.insertAssessment(classroomId, id);
        }
    }

    @Override
    @Transactional
    public void removeAssessments(UUID classroomId, AssessmentIdsRequest request) {
        ensureClassroom(classroomId);
        if (request.getAssessmentIds() == null || request.getAssessmentIds().isEmpty()) {
            throw ApiException.badRequest("assessmentIds must not be empty");
        }
        relationMapper.deleteAssessments(classroomId, request.getAssessmentIds());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> bySubject(UUID subjectId) {
        return classroomMapper.findBySubject(subjectId).stream().map(this::toBasic).toList();
    }

    @Override
    @Transactional
    public void assignTeachers(UUID classroomId, UuidListRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.getIds()) {
            relationMapper.insertTeacher(classroomId, id);
        }
    }

    @Override
    @Transactional
    public void removeTeachers(UUID classroomId, UuidListRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.getIds()) {
            relationMapper.deleteTeacher(classroomId, id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listTeacherIds(UUID classroomId) {
        ensureClassroom(classroomId);
        return relationMapper.listTeacherIds(classroomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> listWithTeachers() {
        List<ClassroomEntity> rooms = classroomMapper.findAll();
        Map<UUID, List<UUID>> teachers = groupTeachers(relationMapper.listAllTeacherAssignments());
        List<ClassroomResponse> out = new ArrayList<>();
        for (ClassroomEntity c : rooms) {
            out.add(toWithRelations(c, teachers.getOrDefault(c.getClassroomId(), List.of()), List.of(), List.of()));
        }
        return out;
    }

    @Override
    @Transactional
    public void assignSubject(UUID classroomId, UUID subjectId) {
        ensureClassroom(classroomId);
        relationMapper.insertSubject(classroomId, subjectId);
    }

    @Override
    @Transactional
    public void removeSubject(UUID classroomId, UUID subjectId) {
        ensureClassroom(classroomId);
        relationMapper.deleteSubject(classroomId, subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listSubjectIds(UUID classroomId) {
        ensureClassroom(classroomId);
        return relationMapper.listSubjectIds(classroomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> listWithSubjects() {
        List<ClassroomEntity> rooms = classroomMapper.findAll();
        Map<UUID, List<UUID>> subjects = groupSubjects(relationMapper.listAllSubjectAssignments());
        List<ClassroomResponse> out = new ArrayList<>();
        for (ClassroomEntity c : rooms) {
            out.add(toWithRelations(c, List.of(), List.of(), subjects.getOrDefault(c.getClassroomId(), List.of())));
        }
        return out;
    }

    @Override
    @Transactional
    public void assignStudents(UUID classroomId, UuidListRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.getIds()) {
            relationMapper.insertStudent(classroomId, id);
        }
    }

    @Override
    @Transactional
    public void removeStudents(UUID classroomId, UuidListRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.getIds()) {
            relationMapper.deleteStudent(classroomId, id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listStudentIds(UUID classroomId) {
        ensureClassroom(classroomId);
        return relationMapper.listStudentIds(classroomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> listWithStudents() {
        List<ClassroomEntity> rooms = classroomMapper.findAll();
        Map<UUID, List<UUID>> students = groupStudents(relationMapper.listAllStudentAssignments());
        List<ClassroomResponse> out = new ArrayList<>();
        for (ClassroomEntity c : rooms) {
            out.add(toWithRelations(c, List.of(), students.getOrDefault(c.getClassroomId(), List.of()), List.of()));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomStudentCountResponse> studentCounts() {
        return relationMapper.studentCounts();
    }

    @Override
    @Transactional
    public void moveStudent(UUID sourceClassroomId, UUID targetClassroomId, UUID studentId) {
        ClassroomEntity src = classroomMapper.findById(sourceClassroomId);
        ClassroomEntity dst = classroomMapper.findById(targetClassroomId);
        if (src == null || dst == null) {
            throw ApiException.notFound("Classroom not found");
        }
        if (src.getAcademicYearId() == null
                || !src.getAcademicYearId().equals(dst.getAcademicYearId())) {
            throw ApiException.badRequest("Classrooms must belong to the same academic year");
        }
        AcademicYearEntity year = academicYearMapper.findById(src.getAcademicYearId());
        if (year == null || !YearStatus.ACTIVE.name().equals(year.getStatus())) {
            throw ApiException.badRequest("Academic year must be active");
        }
        relationMapper.deleteStudent(sourceClassroomId, studentId);
        relationMapper.insertStudent(targetClassroomId, studentId);
    }

    private void ensureClassroom(UUID classroomId) {
        if (classroomMapper.findById(classroomId) == null) {
            throw ApiException.notFound("Classroom not found");
        }
    }

    private Map<UUID, List<UUID>> groupTeachers(List<ClassroomTeacherRow> rows) {
        Map<UUID, List<UUID>> map = new HashMap<>();
        for (ClassroomTeacherRow r : rows) {
            map.computeIfAbsent(r.getClassroomId(), k -> new ArrayList<>()).add(r.getTeacherId());
        }
        return map;
    }

    private Map<UUID, List<UUID>> groupStudents(List<ClassroomStudentRow> rows) {
        Map<UUID, List<UUID>> map = new HashMap<>();
        for (ClassroomStudentRow r : rows) {
            map.computeIfAbsent(r.getClassroomId(), k -> new ArrayList<>()).add(r.getStudentId());
        }
        return map;
    }

    private Map<UUID, List<UUID>> groupSubjects(List<ClassroomSubjectRow> rows) {
        Map<UUID, List<UUID>> map = new HashMap<>();
        for (ClassroomSubjectRow r : rows) {
            map.computeIfAbsent(r.getClassroomId(), k -> new ArrayList<>()).add(r.getSubjectId());
        }
        return map;
    }

    private ClassroomResponse toBasic(ClassroomEntity e) {
        return ClassroomResponse.builder()
                .classroomId(e.getClassroomId())
                .className(e.getClassName())
                .classroomAbbre(e.getClassroomAbbre())
                .description(e.getDescription())
                .image(e.getImage())
                .academicYearId(e.getAcademicYearId())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private ClassroomResponse toWithRelations(
            ClassroomEntity e, List<UUID> teacherIds, List<UUID> studentIds, List<UUID> subjectIds) {
        ClassroomResponse base = toBasic(e);
        base.setTeacherIds(teacherIds);
        base.setStudentIds(studentIds);
        base.setSubjectIds(subjectIds);
        return base;
    }
}
