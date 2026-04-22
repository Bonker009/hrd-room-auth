package org.kshrd.hrdroomservice.api.dto.classroom;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSummaryResponse {

    private UUID assessmentId;
    private String name;
    private LocalDate assessmentDate;
}
