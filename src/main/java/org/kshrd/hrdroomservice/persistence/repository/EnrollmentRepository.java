package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.classroom.CourseSummary;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {

    int countByStudentIdAndCourseId(UUID studentId, UUID courseId);

    Optional<EnrollmentEntity> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<EnrollmentEntity> findByCourseIdOrderByEnrolledAtDesc(UUID courseId);

    @Query(
            """
            select e
            from EnrollmentEntity e
            join AcademicYearEntity y on y.academicYearId = e.academicYearId
            where y.status = 'ACTIVE'
            order by e.enrolledAt desc
            """)
    List<EnrollmentEntity> findAllActiveYearOnly();

    List<EnrollmentEntity> findAllByOrderByEnrolledAtDesc();

    @Query(
            """
            select e
            from EnrollmentEntity e
            join AcademicYearEntity y on y.academicYearId = e.academicYearId
            where e.studentId = :studentId and y.status = 'ACTIVE'
            order by e.enrolledAt desc
            """)
    List<EnrollmentEntity> findByStudentIdActiveYearOnly(UUID studentId);

    List<EnrollmentEntity> findByStudentIdOrderByEnrolledAtDesc(UUID studentId);

    @Query(
            """
            select distinct e.academicYearId
            from EnrollmentEntity e
            join AcademicYearEntity y on y.academicYearId = e.academicYearId
            where e.studentId = :studentId
              and y.status = 'ACTIVE'
              and coalesce(e.isTerminated, false) = false
            """)
    List<UUID> findActiveAcademicYearsForStudent(UUID studentId);

    @Query(
            """
            select new org.kshrd.hrdroomservice.api.dto.classroom.CourseSummary(
                c.courseId, c.code, c.name, c.type)
            from EnrollmentEntity e
            join CourseEntity c on c.courseId = e.courseId
            join AcademicYearEntity y on y.academicYearId = e.academicYearId
            where e.studentId = :studentId
              and y.status = 'ACTIVE'
              and coalesce(e.isTerminated, false) = false
            order by e.enrolledAt desc
            """)
    List<CourseSummary> findCurrentCoursesForStudent(UUID studentId);
}
