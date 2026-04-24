package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentDetailView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentDetailRepository extends JpaRepository<EnrollmentDetailView, UUID> {

    List<EnrollmentDetailView> findAllByOrderByEnrolledAtDesc();

    @Query(
            """
            select e from EnrollmentDetailView e
            where e.academicYearStatus = 'ACTIVE'
            order by e.enrolledAt desc
            """)
    List<EnrollmentDetailView> findAllActiveYearOnly();

    List<EnrollmentDetailView> findByStudentIdOrderByEnrolledAtDesc(UUID studentId);

    @Query(
            """
            select e from EnrollmentDetailView e
            where e.studentId = :studentId
              and e.academicYearStatus = 'ACTIVE'
            order by e.enrolledAt desc
            """)
    List<EnrollmentDetailView> findByStudentIdActiveYearOnly(UUID studentId);

    List<EnrollmentDetailView> findByCourseIdOrderByEnrolledAtDesc(UUID courseId);
}
