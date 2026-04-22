package org.kshrd.hrdroomservice.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class CourseEntity {

    private UUID courseId;
    private String code;
    private String name;
    private String type;
    private UUID academicYearId;
    private UUID prerequisiteCourseId;
    private String description;
    private Boolean isArchived;
    private Long version;
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
