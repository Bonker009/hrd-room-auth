package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomStudentCountResponse;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassroomStudentRepository extends JpaRepository<ClassroomStudentEntity, UUID> {

    boolean existsByClassroomIdAndStudentId(UUID classroomId, UUID studentId);

    int deleteByClassroomIdAndStudentId(UUID classroomId, UUID studentId);

    int deleteByClassroomId(UUID classroomId);

    List<ClassroomStudentEntity> findByClassroomIdOrderByAssignedAt(UUID classroomId);

    List<ClassroomStudentEntity> findAll();

    @Query(
            """
            select distinct c.academicYearId
            from ClassroomStudentEntity cs
            join ClassroomEntity c on c.classroomId = cs.classroomId
            join AcademicYearEntity y on y.academicYearId = c.academicYearId
            where cs.studentId = :studentId and y.status = 'ACTIVE'
            """)
    List<UUID> findActiveAcademicYearsForStudent(UUID studentId);

    @Query(
            """
            select new org.kshrd.hrdroomservice.api.dto.classroom.ClassroomStudentCountResponse(
                cs.classroomId, count(cs))
            from ClassroomStudentEntity cs
            group by cs.classroomId
            """)
    List<ClassroomStudentCountResponse> countStudentsPerClassroom();
}
