package org.kshrd.hrdroomservice.api.dto.enrollment;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnrollmentResponse(
        UUID enrollmentId,
        UUID studentId,
        UUID courseId,
        String courseName,
        String courseType,
        UUID academicYearId,
        String academicYearName,
        Integer generation,
        LocalDateTime enrolledAt,
        Boolean isPassed,
        Boolean isTerminated,
        LocalDateTime terminatedAt,
        String terminationReason,
        Long version) {}
