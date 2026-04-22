package org.kshrd.hrdroomservice.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ClassroomEntity {

    private UUID classroomId;
    private String className;
    private String classroomAbbre;
    private String description;
    private String image;
    private UUID academicYearId;
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
