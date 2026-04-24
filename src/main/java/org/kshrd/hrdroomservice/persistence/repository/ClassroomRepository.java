package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassroomRepository extends JpaRepository<ClassroomEntity, UUID> {

    @Query(
            """
            select c from ClassroomEntity c
            order by c.createdAt desc
            """)
    List<ClassroomEntity> pageAll(Pageable pageable);

    @Query(
            """
            select distinct c
            from ClassroomEntity c
            join ClassroomTeacherEntity ct on ct.classroomId = c.classroomId
            where ct.teacherId = :teacherId
            order by c.createdAt desc
            """)
    List<ClassroomEntity> pageForTeacher(UUID teacherId, Pageable pageable);

    @Query(
            """
            select count(distinct c.classroomId)
            from ClassroomEntity c
            join ClassroomTeacherEntity ct on ct.classroomId = c.classroomId
            where ct.teacherId = :teacherId
            """)
    long countForTeacher(UUID teacherId);

    @Query(
            """
            select c
            from ClassroomEntity c
            join ClassroomSubjectEntity cs on cs.classroomId = c.classroomId
            where cs.subjectId = :subjectId
            order by c.createdAt desc
            """)
    List<ClassroomEntity> findBySubject(UUID subjectId);

    List<ClassroomEntity> findAllByOrderByCreatedAtDesc();
}
