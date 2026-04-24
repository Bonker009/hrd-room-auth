package org.kshrd.hrdroomservice.api.dto.classroom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClassroomResponse(
        UUID classroomId,
        String className,
        String classroomAbbre,
        String description,
        String image,
        UUID academicYearId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<UUID> teacherIds,
        List<UUID> studentIds,
        List<UUID> subjectIds) {}
