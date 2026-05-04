package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentDetailView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentDetailRepository extends JpaRepository<EnrollmentDetailView, UUID> {

    List<EnrollmentDetailView> findAllByOrderByEnrolledAtDesc();

    Page<EnrollmentDetailView> findAllByOrderByEnrolledAtDesc(Pageable pageable);

    @Query(
            """
            select e from EnrollmentDetailView e
            where e.academicYearStatus = 'ACTIVE'
            order by e.enrolledAt desc
            """)
    List<EnrollmentDetailView> findAllActiveYearOnly();

    @Query(
            value =
                    """
                    select e from EnrollmentDetailView e
                    where e.academicYearStatus = 'ACTIVE'
                    order by e.enrolledAt desc
                    """,
            countQuery =
                    """
                    select count(e) from EnrollmentDetailView e
                    where e.academicYearStatus = 'ACTIVE'
                    """)
    Page<EnrollmentDetailView> pageAllActiveYearOnly(Pageable pageable);

    List<EnrollmentDetailView> findByStudentIdOrderByEnrolledAtDesc(UUID studentId);

    Page<EnrollmentDetailView> findByStudentIdOrderByEnrolledAtDesc(
            UUID studentId, Pageable pageable);

    @Query(
            """
            select e from EnrollmentDetailView e
            where e.studentId = :studentId
              and e.academicYearStatus = 'ACTIVE'
            order by e.enrolledAt desc
            """)
    List<EnrollmentDetailView> findByStudentIdActiveYearOnly(UUID studentId);

    @Query(
            value =
                    """
                    select e from EnrollmentDetailView e
                    where e.studentId = :studentId
                      and e.academicYearStatus = 'ACTIVE'
                    order by e.enrolledAt desc
                    """,
            countQuery =
                    """
                    select count(e) from EnrollmentDetailView e
                    where e.studentId = :studentId
                      and e.academicYearStatus = 'ACTIVE'
                    """)
    Page<EnrollmentDetailView> pageByStudentIdActiveYearOnly(UUID studentId, Pageable pageable);

    List<EnrollmentDetailView> findByCourseIdOrderByEnrolledAtDesc(UUID courseId);

    Page<EnrollmentDetailView> findByCourseIdOrderByEnrolledAtDesc(
            UUID courseId, Pageable pageable);
}
