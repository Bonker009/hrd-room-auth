package org.kshrd.hrdroomservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "assessment_classroom")
public class AssessmentClassroomEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;
}
