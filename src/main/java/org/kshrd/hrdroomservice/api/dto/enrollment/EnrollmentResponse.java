package org.kshrd.hrdroomservice.api.dto.enrollment;

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
public class EnrollmentResponse {

    private UUID enrollmentId;
    private UUID studentId;
    private UUID courseId;
    private String courseName;
    private String courseType;
    private UUID academicYearId;
    private String academicYearName;
    private LocalDateTime enrolledAt;
    private Boolean isPassed;
    private Boolean isTerminated;
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private Long version;
}
