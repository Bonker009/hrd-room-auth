package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomTeacherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomTeacherRepository extends JpaRepository<ClassroomTeacherEntity, UUID> {

    boolean existsByClassroomIdAndTeacherId(UUID classroomId, UUID teacherId);

    int deleteByClassroomIdAndTeacherId(UUID classroomId, UUID teacherId);

    int deleteByClassroomId(UUID classroomId);

    List<ClassroomTeacherEntity> findByClassroomIdOrderByAssignedAt(UUID classroomId);

    List<ClassroomTeacherEntity> findAll();
}
