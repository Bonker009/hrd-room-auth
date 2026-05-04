package org.kshrd.hrdroomservice.service.enrollment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentByCourseTypeResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.TerminateEnrollmentRequest;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.CourseType;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.mapper.EnrollmentEntityMapper;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentDetailView;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentEntity;
import org.kshrd.hrdroomservice.persistence.repository.AcademicYearRepository;
import org.kshrd.hrdroomservice.persistence.repository.ClassroomStudentRepository;
import org.kshrd.hrdroomservice.persistence.repository.CourseRepository;
import org.kshrd.hrdroomservice.persistence.repository.EnrollmentDetailRepository;
import org.kshrd.hrdroomservice.persistence.repository.EnrollmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentDetailRepository enrollmentDetailRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final EnrollmentEntityMapper enrollmentMapper;

    @Override
    @Transactional
    public EnrollmentResponse enroll(UUID studentId, UUID courseId, UUID actorId) {
        if (enrollmentRepository.countByStudentIdAndCourseId(studentId, courseId) > 0) {
            throw ApiException.conflict("Student is already enrolled in this course");
        }
        CourseEntity course = requireActiveCourse(courseId);
        validateEnrollmentEligibility(studentId, course);
        validateAdvancedRules(studentId, course);
        EnrollmentEntity row = enrollmentMapper.toNewEntity(studentId, courseId, course);
        enrollmentRepository.save(row);
        return toResponse(
                enrollmentDetailRepository
                        .findById(row.getEnrollmentId())
                        .orElseThrow(() -> ApiException.notFound("Enrollment not found")));
    }

    private CourseEntity requireActiveCourse(UUID courseId) {
        CourseEntity course =
                courseRepository
                        .findById(courseId)
                        .orElseThrow(() -> ApiException.notFound("Course not found"));
        AcademicYearEntity year =
                academicYearRepository.findById(course.getAcademicYearId()).orElse(null);
        if (year == null || !YearStatus.ACTIVE.name().equals(year.getStatus())) {
            throw ApiException.badRequest("Course academic year must be active");
        }
        return course;
    }

    private void validateEnrollmentEligibility(UUID studentId, CourseEntity course) {
        UUID fromEnrollments =
                enrollmentRepository.findActiveAcademicYearsForStudent(studentId).stream()
                        .findFirst()
                        .orElse(null);
        UUID fromClassrooms =
                classroomStudentRepository.findActiveAcademicYearsForStudent(studentId).stream()
                        .findFirst()
                        .orElse(null);
        UUID currentYear = firstNonNull(fromEnrollments, fromClassrooms);
        if (currentYear != null && !currentYear.equals(course.getAcademicYearId())) {
            throw ApiException.badRequest(
                    "Student academic year does not match course academic year");
        }
    }

    private static UUID firstNonNull(UUID a, UUID b) {
        if (a != null) {
            return a;
        }
        return b;
    }

    private void validateAdvancedRules(UUID studentId, CourseEntity course) {
        if (!CourseType.ADVANCED.name().equals(course.getType())) {
            return;
        }
        if (course.getPrerequisiteCourseId() == null) {
            throw ApiException.badRequest("ADVANCED course is missing prerequisite configuration");
        }
        EnrollmentEntity prereq =
                enrollmentRepository
                        .findByStudentIdAndCourseId(studentId, course.getPrerequisiteCourseId())
                        .orElse(null);
        if (prereq == null) {
            throw ApiException.badRequest("Prerequisite enrollment not found");
        }
        if (Boolean.TRUE.equals(prereq.getIsTerminated())) {
            throw ApiException.badRequest("Terminated students cannot enroll in ADVANCED");
        }
        if (!Boolean.TRUE.equals(prereq.getIsPassed())) {
            throw ApiException.badRequest(
                    "Prerequisite course must be passed before ADVANCED enrollment");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> listAll(boolean includeArchived, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return PageResponse.of(
                includeArchived
                        ? enrollmentDetailRepository.findAllByOrderByEnrolledAtDesc(pageable)
                        : enrollmentDetailRepository.pageAllActiveYearOnly(pageable),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentByCourseTypeResponse listByCourseType(boolean includeArchived) {
        List<EnrollmentDetailView> rows =
                includeArchived
                        ? enrollmentDetailRepository.findAllByOrderByEnrolledAtDesc()
                        : enrollmentDetailRepository.findAllActiveYearOnly();
        return partitionByType(rows.stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> listByStudent(
            UUID studentId, boolean includeArchived, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return PageResponse.of(
                includeArchived
                        ? enrollmentDetailRepository.findByStudentIdOrderByEnrolledAtDesc(
                                studentId, pageable)
                        : enrollmentDetailRepository.pageByStudentIdActiveYearOnly(
                                studentId, pageable),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentByCourseTypeResponse listByStudentGrouped(
            UUID studentId, boolean includeArchived) {
        List<EnrollmentDetailView> rows =
                includeArchived
                        ? enrollmentDetailRepository.findByStudentIdOrderByEnrolledAtDesc(studentId)
                        : enrollmentDetailRepository.findByStudentIdActiveYearOnly(studentId);
        return partitionByType(rows.stream().map(this::toResponse).toList());
    }

    private EnrollmentByCourseTypeResponse partitionByType(List<EnrollmentResponse> rows) {
        List<EnrollmentResponse> basic = new ArrayList<>();
        List<EnrollmentResponse> advanced = new ArrayList<>();
        for (EnrollmentResponse r : rows) {
            if (CourseType.BASIC.name().equals(r.courseType())) {
                basic.add(r);
            } else if (CourseType.ADVANCED.name().equals(r.courseType())) {
                advanced.add(r);
            }
        }
        return new EnrollmentByCourseTypeResponse(basic, advanced);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> listByCourse(UUID courseId, int page, int size) {
        return PageResponse.of(
                enrollmentDetailRepository.findByCourseIdOrderByEnrolledAtDesc(
                        courseId, PageRequest.of(page, size)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getById(UUID enrollmentId) {
        return toResponse(
                enrollmentDetailRepository
                        .findById(enrollmentId)
                        .orElseThrow(() -> ApiException.notFound("Enrollment not found")));
    }

    @Override
    @Transactional
    public EnrollmentResponse terminate(
            UUID enrollmentId, TerminateEnrollmentRequest request, UUID actorId) {
        EnrollmentEntity row =
                enrollmentRepository
                        .findById(enrollmentId)
                        .orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        row.setIsTerminated(true);
        row.setTerminatedAt(LocalDateTime.now());
        row.setTerminationReason(request == null ? null : request.reason());
        try {
            enrollmentRepository.saveAndFlush(row);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            throw ApiException.conflict("Enrollment was modified concurrently");
        }
        return toResponse(enrollmentDetailRepository.findById(enrollmentId).orElseThrow());
    }

    @Override
    @Transactional
    public EnrollmentResponse reactivate(UUID enrollmentId, UUID actorId) {
        EnrollmentEntity row =
                enrollmentRepository
                        .findById(enrollmentId)
                        .orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        row.setIsTerminated(false);
        row.setTerminatedAt(null);
        row.setTerminationReason(null);
        try {
            enrollmentRepository.saveAndFlush(row);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            throw ApiException.conflict("Enrollment was modified concurrently");
        }
        return toResponse(enrollmentDetailRepository.findById(enrollmentId).orElseThrow());
    }

    @Override
    @Transactional
    public EnrollmentResponse moveToAdvanced(
            UUID studentId, UUID basicCourseId, UUID advancedCourseId, UUID actorId) {
        CourseEntity advanced =
                courseRepository
                        .findById(advancedCourseId)
                        .orElseThrow(() -> ApiException.notFound("Advanced course not found"));
        if (advanced.getPrerequisiteCourseId() == null
                || !advanced.getPrerequisiteCourseId().equals(basicCourseId)) {
            throw ApiException.badRequest(
                    "Advanced course prerequisite does not match basic course");
        }
        EnrollmentEntity basicEnrollment =
                enrollmentRepository
                        .findByStudentIdAndCourseId(studentId, basicCourseId)
                        .orElse(null);
        if (basicEnrollment == null) {
            throw ApiException.badRequest("Basic enrollment not found");
        }
        if (enrollmentRepository.countByStudentIdAndCourseId(studentId, advancedCourseId) > 0) {
            throw ApiException.conflict("Student is already enrolled in the advanced course");
        }
        basicEnrollment.setIsPassed(true);
        basicEnrollment.setCompletedAt(LocalDateTime.now());
        try {
            enrollmentRepository.saveAndFlush(basicEnrollment);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            throw ApiException.conflict("Basic enrollment was modified concurrently");
        }
        return enroll(studentId, advancedCourseId, actorId);
    }

    private EnrollmentResponse toResponse(EnrollmentDetailView e) {
        return new EnrollmentResponse(
                e.getEnrollmentId(),
                e.getStudentId(),
                e.getCourseId(),
                e.getCourseName(),
                e.getCourseType(),
                e.getAcademicYearId(),
                e.getAcademicYearName(),
                e.getGeneration(),
                e.getEnrolledAt(),
                e.getIsPassed(),
                e.getIsTerminated(),
                e.getTerminatedAt(),
                e.getTerminationReason(),
                e.getVersion());
    }
}
