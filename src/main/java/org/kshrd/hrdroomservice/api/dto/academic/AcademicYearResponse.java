package org.kshrd.hrdroomservice.api.dto.academic;

import java.time.LocalDateTime;
import java.util.UUID;

public record AcademicYearResponse(
        UUID academicYearId,
        String name,
        Integer generation,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
