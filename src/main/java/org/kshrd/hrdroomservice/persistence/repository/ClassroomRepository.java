package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassroomRepository extends JpaRepository<ClassroomEntity, UUID> {

    @Query(
            """
            select c from ClassroomEntity c
            order by c.createdAt desc
            """)
    Page<ClassroomEntity> pageAll(Pageable pageable);

    @Query(
            value =
                    """
                    select distinct c
                    from ClassroomEntity c
                    join ClassroomTeacherEntity ct on ct.classroomId = c.classroomId
                    where ct.teacherId = :teacherId
                    order by c.createdAt desc
                    """,
            countQuery =
                    """
                    select count(distinct c.classroomId)
                    from ClassroomEntity c
                    join ClassroomTeacherEntity ct on ct.classroomId = c.classroomId
                    where ct.teacherId = :teacherId
                    """)
    Page<ClassroomEntity> pageForTeacher(UUID teacherId, Pageable pageable);

    @Query(
            """
            select c
            from ClassroomEntity c
            join ClassroomSubjectEntity cs on cs.classroomId = c.classroomId
            where cs.subjectId = :subjectId
            order by c.createdAt desc
            """)
    List<ClassroomEntity> findBySubject(UUID subjectId);

    @Query(
            """
            select c
            from ClassroomEntity c
            join ClassroomStudentEntity cs on cs.classroomId = c.classroomId
            join AcademicYearEntity y on y.academicYearId = c.academicYearId
            where cs.studentId = :studentId and y.status = 'ACTIVE'
            order by cs.assignedAt desc
            """)
    List<ClassroomEntity> findCurrentForStudent(UUID studentId, Pageable pageable);

    List<ClassroomEntity> findAllByOrderByCreatedAtDesc();
}
