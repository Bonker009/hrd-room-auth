package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomSubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomSubjectRepository extends JpaRepository<ClassroomSubjectEntity, UUID> {

    boolean existsByClassroomIdAndSubjectId(UUID classroomId, UUID subjectId);

    int deleteByClassroomIdAndSubjectId(UUID classroomId, UUID subjectId);

    int deleteByClassroomId(UUID classroomId);

    List<ClassroomSubjectEntity> findByClassroomId(UUID classroomId);

    List<ClassroomSubjectEntity> findAll();
}
