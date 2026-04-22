package org.kshrd.hrdroomservice.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class AcademicYearEntity {

    private UUID academicYearId;
    private String name;
    private Integer generation;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long version;
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
