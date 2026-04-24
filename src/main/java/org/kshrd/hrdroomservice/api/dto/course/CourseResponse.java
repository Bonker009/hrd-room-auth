package org.kshrd.hrdroomservice.api.dto.course;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseResponse(
        UUID courseId,
        String code,
        String name,
        String type,
        UUID academicYearId,
        UUID prerequisiteCourseId,
        String description,
        Boolean isArchived,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
