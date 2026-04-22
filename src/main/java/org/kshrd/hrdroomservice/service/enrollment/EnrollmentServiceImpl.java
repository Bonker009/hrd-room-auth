package org.kshrd.hrdroomservice.service.enrollment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentByCourseTypeResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.TerminateEnrollmentRequest;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.CourseType;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentEntity;
import org.kshrd.hrdroomservice.persistence.mapper.AcademicYearMapper;
import org.kshrd.hrdroomservice.persistence.mapper.ClassroomRelationMapper;
import org.kshrd.hrdroomservice.persistence.mapper.CourseMapper;
import org.kshrd.hrdroomservice.persistence.mapper.EnrollmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final AcademicYearMapper academicYearMapper;
    private final ClassroomRelationMapper classroomRelationMapper;

    @Override
    @Transactional
    public EnrollmentResponse enroll(UUID studentId, UUID courseId, UUID actorId) {
        if (enrollmentMapper.countByStudentAndCourse(studentId, courseId) > 0) {
            throw ApiException.conflict("Student is already enrolled in this course");
        }
        CourseEntity course = requireActiveCourse(courseId);
        validateEnrollmentEligibility(studentId, course);
        validateAdvancedRules(studentId, course);
        EnrollmentEntity row = new EnrollmentEntity();
        row.setEnrollmentId(UUID.randomUUID());
        row.setStudentId(studentId);
        row.setCourseId(courseId);
        row.setAcademicYearId(course.getAcademicYearId());
        row.setEnrolledAt(LocalDateTime.now());
        row.setIsPassed(false);
        row.setIsTerminated(false);
        row.setVersion(0L);
        row.setCreatedBy(actorId);
        row.setUpdatedBy(actorId);
        row.setCreatedAt(LocalDateTime.now());
        enrollmentMapper.insert(row);
        return toResponse(enrollmentMapper.findById(row.getEnrollmentId()));
    }

    private CourseEntity requireActiveCourse(UUID courseId) {
        CourseEntity course = courseMapper.findById(courseId);
        if (course == null) {
            throw ApiException.notFound("Course not found");
        }
        AcademicYearEntity year = academicYearMapper.findById(course.getAcademicYearId());
        if (year == null || !YearStatus.ACTIVE.name().equals(year.getStatus())) {
            throw ApiException.badRequest("Course academic year must be active");
        }
        return course;
    }

    private void validateEnrollmentEligibility(UUID studentId, CourseEntity course) {
        UUID fromEnrollments = enrollmentMapper.findActiveAcademicYearForStudentFromEnrollments(studentId);
        UUID fromClassrooms = classroomRelationMapper.findActiveAcademicYearForStudentFromClassrooms(studentId);
        UUID currentYear = firstNonNull(fromEnrollments, fromClassrooms);
        if (currentYear != null && !currentYear.equals(course.getAcademicYearId())) {
            throw ApiException.badRequest("Student academic year does not match course academic year");
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
                enrollmentMapper.findByStudentAndCourse(studentId, course.getPrerequisiteCourseId());
        if (prereq == null) {
            throw ApiException.badRequest("Prerequisite enrollment not found");
        }
        if (Boolean.TRUE.equals(prereq.getIsTerminated())) {
            throw ApiException.badRequest("Terminated students cannot enroll in ADVANCED");
        }
        if (!Boolean.TRUE.equals(prereq.getIsPassed())) {
            throw ApiException.badRequest("Prerequisite course must be passed before ADVANCED enrollment");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listAll(boolean includeArchived) {
        return enrollmentMapper.findAll(includeArchived).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentByCourseTypeResponse listByCourseType(boolean includeArchived) {
        List<EnrollmentResponse> all = listAll(includeArchived);
        return partitionByType(all);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listByStudent(UUID studentId, boolean includeArchived) {
        return enrollmentMapper.findByStudent(studentId, includeArchived).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentByCourseTypeResponse listByStudentGrouped(UUID studentId, boolean includeArchived) {
        return partitionByType(listByStudent(studentId, includeArchived));
    }

    private EnrollmentByCourseTypeResponse partitionByType(List<EnrollmentResponse> rows) {
        List<EnrollmentResponse> basic = new ArrayList<>();
        List<EnrollmentResponse> advanced = new ArrayList<>();
        for (EnrollmentResponse r : rows) {
            if (CourseType.BASIC.name().equals(r.getCourseType())) {
                basic.add(r);
            } else if (CourseType.ADVANCED.name().equals(r.getCourseType())) {
                advanced.add(r);
            }
        }
        return EnrollmentByCourseTypeResponse.builder().basic(basic).advanced(advanced).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listByCourse(UUID courseId) {
        return enrollmentMapper.findByCourse(courseId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getById(UUID enrollmentId) {
        EnrollmentEntity row = enrollmentMapper.findById(enrollmentId);
        if (row == null) {
            throw ApiException.notFound("Enrollment not found");
        }
        return toResponse(row);
    }

    @Override
    @Transactional
    public EnrollmentResponse terminate(UUID enrollmentId, TerminateEnrollmentRequest request, UUID actorId) {
        EnrollmentEntity row = enrollmentMapper.findById(enrollmentId);
        if (row == null) {
            throw ApiException.notFound("Enrollment not found");
        }
        long version = row.getVersion() == null ? 0L : row.getVersion();
        int updated =
                enrollmentMapper.terminate(
                        enrollmentId, request == null ? null : request.getReason(), version, actorId);
        if (updated == 0) {
            throw ApiException.conflict("Enrollment was modified concurrently");
        }
        return toResponse(enrollmentMapper.findById(enrollmentId));
    }

    @Override
    @Transactional
    public EnrollmentResponse reactivate(UUID enrollmentId, UUID actorId) {
        EnrollmentEntity row = enrollmentMapper.findById(enrollmentId);
        if (row == null) {
            throw ApiException.notFound("Enrollment not found");
        }
        long version = row.getVersion() == null ? 0L : row.getVersion();
        int updated = enrollmentMapper.reactivate(enrollmentId, version, actorId);
        if (updated == 0) {
            throw ApiException.conflict("Enrollment was modified concurrently");
        }
        return toResponse(enrollmentMapper.findById(enrollmentId));
    }

    @Override
    @Transactional
    public EnrollmentResponse moveToAdvanced(
            UUID studentId, UUID basicCourseId, UUID advancedCourseId, UUID actorId) {
        CourseEntity advanced = courseMapper.findById(advancedCourseId);
        if (advanced == null) {
            throw ApiException.notFound("Advanced course not found");
        }
        if (advanced.getPrerequisiteCourseId() == null
                || !advanced.getPrerequisiteCourseId().equals(basicCourseId)) {
            throw ApiException.badRequest("Advanced course prerequisite does not match basic course");
        }
        EnrollmentEntity basicEnrollment = enrollmentMapper.findByStudentAndCourse(studentId, basicCourseId);
        if (basicEnrollment == null) {
            throw ApiException.badRequest("Basic enrollment not found");
        }
        if (enrollmentMapper.countByStudentAndCourse(studentId, advancedCourseId) > 0) {
            throw ApiException.conflict("Student is already enrolled in the advanced course");
        }
        long basicVersion = basicEnrollment.getVersion() == null ? 0L : basicEnrollment.getVersion();
        int marked =
                enrollmentMapper.markPassed(basicEnrollment.getEnrollmentId(), basicVersion, actorId);
        if (marked == 0) {
            throw ApiException.conflict("Basic enrollment was modified concurrently");
        }
        return enroll(studentId, advancedCourseId, actorId);
    }

    private EnrollmentResponse toResponse(EnrollmentEntity e) {
        CourseEntity course = courseMapper.findById(e.getCourseId());
        AcademicYearEntity year =
                course == null ? null : academicYearMapper.findById(course.getAcademicYearId());
        return EnrollmentResponse.builder()
                .enrollmentId(e.getEnrollmentId())
                .studentId(e.getStudentId())
                .courseId(e.getCourseId())
                .courseName(course == null ? null : course.getName())
                .courseType(course == null ? null : course.getType())
                .academicYearId(e.getAcademicYearId())
                .academicYearName(year == null ? null : year.getName())
                .enrolledAt(e.getEnrolledAt())
                .isPassed(e.getIsPassed())
                .isTerminated(e.getIsTerminated())
                .terminatedAt(e.getTerminatedAt())
                .terminationReason(e.getTerminationReason())
                .version(e.getVersion())
                .build();
    }
}
