package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.classroom.AssessmentSummaryResponse;
import org.kshrd.hrdroomservice.persistence.entity.AssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    @Query(
            """
            select new org.kshrd.hrdroomservice.api.dto.classroom.AssessmentSummaryResponse(
                a.assessmentId, a.name, a.assessmentDate)
            from AssessmentEntity a
            join AssessmentClassroomEntity ac on ac.assessmentId = a.assessmentId
            where ac.classroomId = :classroomId
            order by ac.assignedAt desc
            """)
    List<AssessmentSummaryResponse> findSummaryByClassroomId(UUID classroomId);
}
