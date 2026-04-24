package org.kshrd.hrdroomservice.api.dto.classroom;

import java.time.LocalDate;
import java.util.UUID;

public record AssessmentSummaryResponse(UUID assessmentId, String name, LocalDate assessmentDate) {}
