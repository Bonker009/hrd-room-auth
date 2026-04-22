package org.kshrd.hrdroomservice.api.dto.academic;

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
public class AcademicYearResponse {

    private UUID academicYearId;
    private String name;
    private Integer generation;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
