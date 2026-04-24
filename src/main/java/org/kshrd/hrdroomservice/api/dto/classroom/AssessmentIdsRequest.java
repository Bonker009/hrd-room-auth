package org.kshrd.hrdroomservice.api.dto.classroom;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record AssessmentIdsRequest(@NotEmpty List<UUID> assessmentIds) {}
