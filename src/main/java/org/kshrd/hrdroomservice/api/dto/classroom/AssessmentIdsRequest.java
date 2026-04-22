package org.kshrd.hrdroomservice.api.dto.classroom;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class AssessmentIdsRequest {

    @NotEmpty private List<UUID> assessmentIds;
}
