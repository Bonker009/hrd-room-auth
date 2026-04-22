package org.kshrd.hrdroomservice.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class EnrollmentEntity {

    private UUID enrollmentId;
    private UUID studentId;
    private UUID courseId;
    private UUID academicYearId;
    private LocalDateTime enrolledAt;
    private Double grade;
    private Boolean isPassed;
    private LocalDateTime completedAt;
    private Boolean isTerminated;
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private Long version;
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
