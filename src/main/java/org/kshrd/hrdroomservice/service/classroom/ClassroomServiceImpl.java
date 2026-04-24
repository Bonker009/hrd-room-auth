package org.kshrd.hrdroomservice.service.classroom;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import org.kshrd.hrdroomservice.mapper.ClassroomEntityMapper;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.AssessmentClassroomEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomStudentEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomStudentRow;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomSubjectEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomSubjectRow;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomTeacherEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomTeacherRow;
import org.kshrd.hrdroomservice.persistence.repository.AcademicYearRepository;
import org.kshrd.hrdroomservice.persistence.repository.AssessmentClassroomRepository;
import org.kshrd.hrdroomservice.persistence.repository.AssessmentRepository;
import org.kshrd.hrdroomservice.persistence.repository.ClassroomRepository;
import org.kshrd.hrdroomservice.persistence.repository.ClassroomStudentRepository;
import org.kshrd.hrdroomservice.persistence.repository.ClassroomSubjectRepository;
import org.kshrd.hrdroomservice.persistence.repository.ClassroomTeacherRepository;
import org.kshrd.hrdroomservice.storage.FileStorageService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;

    private final ClassroomRepository classroomRepository;
    private final ClassroomTeacherRepository classroomTeacherRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomSubjectRepository classroomSubjectRepository;
    private final AssessmentClassroomRepository assessmentClassroomRepository;
    private final AssessmentRepository assessmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FileStorageService fileStorageService;
    private final ClassroomEntityMapper classroomMapper;

    @Override
    @Transactional
    public ClassroomResponse create(ClassroomRequest request, UUID actorId) {
        AcademicYearEntity active =
                academicYearRepository.findFirstByStatus(YearStatus.ACTIVE.name()).orElse(null);
        if (active == null) {
            throw ApiException.badRequest("No active academic year");
        }
        ClassroomEntity row = classroomMapper.toNewEntity(request, active);
        classroomRepository.save(row);
        if (request.subjectId() != null) {
            insertSubjectIfMissing(row.getClassroomId(), request.subjectId());
        }
        return toBasic(classroomRepository.findById(row.getClassroomId()).orElseThrow());
    }

    @Override
    @Transactional
    public ClassroomResponse update(UUID classroomId, ClassroomRequest request, UUID actorId) {
        ClassroomEntity existing =
                classroomRepository
                        .findById(classroomId)
                        .orElseThrow(() -> ApiException.notFound("Classroom not found"));
        existing.setClassName(request.className().trim());
        existing.setClassroomAbbre(request.classroomAbbre().trim());
        existing.setDescription(request.description());
        if (StringUtils.hasText(request.image())) {
            existing.setImage(request.image());
        }
        classroomRepository.save(existing);
        return toBasic(classroomRepository.findById(classroomId).orElseThrow());
    }

    @Override
    @Transactional
    public void delete(UUID classroomId) {
        ClassroomEntity existing = classroomRepository.findById(classroomId).orElse(null);
        if (existing == null) {
            throw ApiException.notFound("Classroom not found");
        }
        assessmentClassroomRepository.deleteByClassroomId(classroomId);
        classroomStudentRepository.deleteByClassroomId(classroomId);
        classroomSubjectRepository.deleteByClassroomId(classroomId);
        classroomTeacherRepository.deleteByClassroomId(classroomId);
        classroomRepository.deleteById(classroomId);
    }

    @Override
    @Transactional
    public String uploadImage(UUID classroomId, MultipartFile file, UUID actorId) {
        ClassroomEntity existing = classroomRepository.findById(classroomId).orElse(null);
        if (existing == null) {
            throw ApiException.notFound("Classroom not found");
        }
        validateImage(file);
        String url = fileStorageService.uploadFile(file, "classrooms").url();
        existing.setImage(url);
        classroomRepository.save(existing);
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
        return toBasic(
                classroomRepository
                        .findById(classroomId)
                        .orElseThrow(() -> ApiException.notFound("Classroom not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentSummaryResponse> listAssessments(UUID classroomId) {
        if (classroomRepository.findById(classroomId).isEmpty()) {
            throw ApiException.notFound("Classroom not found");
        }
        return assessmentRepository.findSummaryByClassroomId(classroomId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassroomResponse> myClassrooms(UUID teacherId, int page, int size) {
        long total = classroomRepository.countForTeacher(teacherId);
        List<ClassroomResponse> content =
                classroomRepository.pageForTeacher(teacherId, PageRequest.of(page, size)).stream()
                        .map(this::toBasic)
                        .toList();
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassroomResponse> listAll(int page, int size) {
        long total = classroomRepository.count();
        List<ClassroomResponse> content =
                classroomRepository.pageAll(PageRequest.of(page, size)).stream()
                        .map(this::toBasic)
                        .toList();
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public void assignAssessments(UUID classroomId, AssessmentIdsRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.assessmentIds()) {
            insertAssessmentIfMissing(classroomId, id);
        }
    }

    @Override
    @Transactional
    public void removeAssessments(UUID classroomId, AssessmentIdsRequest request) {
        ensureClassroom(classroomId);
        if (request.assessmentIds() == null || request.assessmentIds().isEmpty()) {
            throw ApiException.badRequest("assessmentIds must not be empty");
        }
        assessmentClassroomRepository.deleteByClassroomIdAndAssessmentIdIn(
                classroomId, request.assessmentIds());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> bySubject(UUID subjectId) {
        return classroomRepository.findBySubject(subjectId).stream().map(this::toBasic).toList();
    }

    @Override
    @Transactional
    public void assignTeachers(UUID classroomId, UuidListRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.ids()) {
            insertTeacherIfMissing(classroomId, id);
        }
    }

    @Override
    @Transactional
    public void removeTeachers(UUID classroomId, UuidListRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.ids()) {
            classroomTeacherRepository.deleteByClassroomIdAndTeacherId(classroomId, id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listTeacherIds(UUID classroomId) {
        ensureClassroom(classroomId);
        return classroomTeacherRepository.findByClassroomIdOrderByAssignedAt(classroomId).stream()
                .map(ClassroomTeacherEntity::getTeacherId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> listWithTeachers() {
        List<ClassroomEntity> rooms = classroomRepository.findAllByOrderByCreatedAtDesc();
        Map<UUID, List<UUID>> teachers =
                groupTeachers(toTeacherRows(classroomTeacherRepository.findAll()));
        List<ClassroomResponse> out = new ArrayList<>();
        for (ClassroomEntity c : rooms) {
            out.add(
                    toWithRelations(
                            c,
                            teachers.getOrDefault(c.getClassroomId(), List.of()),
                            List.of(),
                            List.of()));
        }
        return out;
    }

    @Override
    @Transactional
    public void assignSubject(UUID classroomId, UUID subjectId) {
        ensureClassroom(classroomId);
        insertSubjectIfMissing(classroomId, subjectId);
    }

    @Override
    @Transactional
    public void removeSubject(UUID classroomId, UUID subjectId) {
        ensureClassroom(classroomId);
        classroomSubjectRepository.deleteByClassroomIdAndSubjectId(classroomId, subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listSubjectIds(UUID classroomId) {
        ensureClassroom(classroomId);
        return classroomSubjectRepository.findByClassroomId(classroomId).stream()
                .map(ClassroomSubjectEntity::getSubjectId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> listWithSubjects() {
        List<ClassroomEntity> rooms = classroomRepository.findAllByOrderByCreatedAtDesc();
        Map<UUID, List<UUID>> subjects =
                groupSubjects(toSubjectRows(classroomSubjectRepository.findAll()));
        List<ClassroomResponse> out = new ArrayList<>();
        for (ClassroomEntity c : rooms) {
            out.add(
                    toWithRelations(
                            c,
                            List.of(),
                            List.of(),
                            subjects.getOrDefault(c.getClassroomId(), List.of())));
        }
        return out;
    }

    @Override
    @Transactional
    public void assignStudents(UUID classroomId, UuidListRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.ids()) {
            insertStudentIfMissing(classroomId, id);
        }
    }

    @Override
    @Transactional
    public void removeStudents(UUID classroomId, UuidListRequest request) {
        ensureClassroom(classroomId);
        for (UUID id : request.ids()) {
            classroomStudentRepository.deleteByClassroomIdAndStudentId(classroomId, id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listStudentIds(UUID classroomId) {
        ensureClassroom(classroomId);
        return classroomStudentRepository.findByClassroomIdOrderByAssignedAt(classroomId).stream()
                .map(ClassroomStudentEntity::getStudentId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> listWithStudents() {
        List<ClassroomEntity> rooms = classroomRepository.findAllByOrderByCreatedAtDesc();
        Map<UUID, List<UUID>> students =
                groupStudents(toStudentRows(classroomStudentRepository.findAll()));
        List<ClassroomResponse> out = new ArrayList<>();
        for (ClassroomEntity c : rooms) {
            out.add(
                    toWithRelations(
                            c,
                            List.of(),
                            students.getOrDefault(c.getClassroomId(), List.of()),
                            List.of()));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomStudentCountResponse> studentCounts() {
        return classroomStudentRepository.countStudentsPerClassroom();
    }

    @Override
    @Transactional
    public void moveStudent(UUID sourceClassroomId, UUID targetClassroomId, UUID studentId) {
        ClassroomEntity src = classroomRepository.findById(sourceClassroomId).orElse(null);
        ClassroomEntity dst = classroomRepository.findById(targetClassroomId).orElse(null);
        if (src == null || dst == null) {
            throw ApiException.notFound("Classroom not found");
        }
        if (src.getAcademicYearId() == null
                || !src.getAcademicYearId().equals(dst.getAcademicYearId())) {
            throw ApiException.badRequest("Classrooms must belong to the same academic year");
        }
        AcademicYearEntity year =
                academicYearRepository.findById(src.getAcademicYearId()).orElse(null);
        if (year == null || !YearStatus.ACTIVE.name().equals(year.getStatus())) {
            throw ApiException.badRequest("Academic year must be active");
        }
        classroomStudentRepository.deleteByClassroomIdAndStudentId(sourceClassroomId, studentId);
        insertStudentIfMissing(targetClassroomId, studentId);
    }

    private void ensureClassroom(UUID classroomId) {
        if (classroomRepository.findById(classroomId).isEmpty()) {
            throw ApiException.notFound("Classroom not found");
        }
    }

    private void insertTeacherIfMissing(UUID classroomId, UUID teacherId) {
        if (classroomTeacherRepository.existsByClassroomIdAndTeacherId(classroomId, teacherId)) {
            return;
        }
        ClassroomTeacherEntity entity = new ClassroomTeacherEntity();
        entity.setId(UUID.randomUUID());
        entity.setClassroomId(classroomId);
        entity.setTeacherId(teacherId);
        entity.setAssignedAt(LocalDateTime.now());
        classroomTeacherRepository.save(entity);
    }

    private void insertStudentIfMissing(UUID classroomId, UUID studentId) {
        if (classroomStudentRepository.existsByClassroomIdAndStudentId(classroomId, studentId)) {
            return;
        }
        ClassroomStudentEntity entity = new ClassroomStudentEntity();
        entity.setId(UUID.randomUUID());
        entity.setClassroomId(classroomId);
        entity.setStudentId(studentId);
        entity.setAssignedAt(LocalDateTime.now());
        classroomStudentRepository.save(entity);
    }

    private void insertSubjectIfMissing(UUID classroomId, UUID subjectId) {
        if (classroomSubjectRepository.existsByClassroomIdAndSubjectId(classroomId, subjectId)) {
            return;
        }
        ClassroomSubjectEntity entity = new ClassroomSubjectEntity();
        entity.setId(UUID.randomUUID());
        entity.setClassroomId(classroomId);
        entity.setSubjectId(subjectId);
        classroomSubjectRepository.save(entity);
    }

    private void insertAssessmentIfMissing(UUID classroomId, UUID assessmentId) {
        if (assessmentClassroomRepository.existsByClassroomIdAndAssessmentId(
                classroomId, assessmentId)) {
            return;
        }
        AssessmentClassroomEntity entity = new AssessmentClassroomEntity();
        entity.setId(UUID.randomUUID());
        entity.setClassroomId(classroomId);
        entity.setAssessmentId(assessmentId);
        entity.setAssignedAt(OffsetDateTime.now());
        assessmentClassroomRepository.save(entity);
    }

    private List<ClassroomTeacherRow> toTeacherRows(List<ClassroomTeacherEntity> entities) {
        return entities.stream()
                .map(
                        e -> {
                            ClassroomTeacherRow row = new ClassroomTeacherRow();
                            row.setClassroomId(e.getClassroomId());
                            row.setTeacherId(e.getTeacherId());
                            return row;
                        })
                .toList();
    }

    private List<ClassroomSubjectRow> toSubjectRows(List<ClassroomSubjectEntity> entities) {
        return entities.stream()
                .map(
                        e -> {
                            ClassroomSubjectRow row = new ClassroomSubjectRow();
                            row.setClassroomId(e.getClassroomId());
                            row.setSubjectId(e.getSubjectId());
                            return row;
                        })
                .toList();
    }

    private List<ClassroomStudentRow> toStudentRows(List<ClassroomStudentEntity> entities) {
        return entities.stream()
                .map(
                        e -> {
                            ClassroomStudentRow row = new ClassroomStudentRow();
                            row.setClassroomId(e.getClassroomId());
                            row.setStudentId(e.getStudentId());
                            return row;
                        })
                .toList();
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
        return toWithRelations(e, null, null, null);
    }

    private ClassroomResponse toWithRelations(
            ClassroomEntity e,
            List<UUID> teacherIds,
            List<UUID> studentIds,
            List<UUID> subjectIds) {
        return new ClassroomResponse(
                e.getClassroomId(),
                e.getClassName(),
                e.getClassroomAbbre(),
                e.getDescription(),
                e.getImage(),
                e.getAcademicYearId(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                teacherIds,
                studentIds,
                subjectIds);
    }
}
