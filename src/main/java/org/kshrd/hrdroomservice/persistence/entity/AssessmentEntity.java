package org.kshrd.hrdroomservice.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class AssessmentEntity {

    private UUID assessmentId;
    private String name;
    private java.time.LocalDate assessmentDate;
    private UUID academicYearId;
    private LocalDateTime createdAt;
}
