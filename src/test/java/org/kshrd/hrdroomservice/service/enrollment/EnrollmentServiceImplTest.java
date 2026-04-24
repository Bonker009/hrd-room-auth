package org.kshrd.hrdroomservice.service.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kshrd.hrdroomservice.api.dto.enrollment.TerminateEnrollmentRequest;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentEntity;
import org.kshrd.hrdroomservice.persistence.repository.AcademicYearRepository;
import org.kshrd.hrdroomservice.persistence.repository.ClassroomStudentRepository;
import org.kshrd.hrdroomservice.persistence.repository.CourseRepository;
import org.kshrd.hrdroomservice.persistence.repository.EnrollmentRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ClassroomStudentRepository classroomStudentRepository;

    @InjectMocks private EnrollmentServiceImpl service;

    @Test
    void enroll_shouldRejectWhenAcademicYearInactive() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();

        CourseEntity course = new CourseEntity();
        course.setCourseId(courseId);
        course.setAcademicYearId(yearId);

        AcademicYearEntity year = new AcademicYearEntity();
        year.setAcademicYearId(yearId);
        year.setStatus(YearStatus.ARCHIVED.name());

        when(enrollmentRepository.countByStudentIdAndCourseId(studentId, courseId)).thenReturn(0);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));

        ApiException ex =
                assertThrows(
                        ApiException.class,
                        () -> service.enroll(studentId, courseId, UUID.randomUUID()));
        assertEquals("Course academic year must be active", ex.getMessage());
    }

    @Test
    void terminate_shouldMapOptimisticLockFailureToConflict() {
        UUID enrollmentId = UUID.randomUUID();
        EnrollmentEntity row = new EnrollmentEntity();
        row.setEnrollmentId(enrollmentId);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(row));
        when(enrollmentRepository.saveAndFlush(any()))
                .thenThrow(
                        new ObjectOptimisticLockingFailureException(
                                EnrollmentEntity.class, enrollmentId));

        ApiException ex =
                assertThrows(
                        ApiException.class,
                        () ->
                                service.terminate(
                                        enrollmentId,
                                        new TerminateEnrollmentRequest("r"),
                                        UUID.randomUUID()));
        assertEquals("Enrollment was modified concurrently", ex.getMessage());
    }
}
