package org.kshrd.hrdroomservice.api.dto.course;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private UUID courseId;
    private String code;
    private String name;
    private String type;
    private UUID academicYearId;
    private UUID prerequisiteCourseId;
    private String description;
    private Boolean isArchived;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
