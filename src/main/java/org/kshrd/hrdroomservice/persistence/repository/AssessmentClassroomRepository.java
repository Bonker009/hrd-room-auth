package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.AssessmentClassroomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentClassroomRepository
        extends JpaRepository<AssessmentClassroomEntity, UUID> {

    boolean existsByClassroomIdAndAssessmentId(UUID classroomId, UUID assessmentId);

    int deleteByClassroomIdAndAssessmentId(UUID classroomId, UUID assessmentId);

    int deleteByClassroomId(UUID classroomId);

    int deleteByClassroomIdAndAssessmentIdIn(UUID classroomId, List<UUID> assessmentIds);

    List<AssessmentClassroomEntity> findByClassroomIdOrderByAssignedAtDesc(UUID classroomId);
}
