package org.kshrd.hrdroomservice.api.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AcademicYearRequest(
                @NotBlank String name,
                Integer generation,
                @NotNull LocalDateTime startDate,
                @NotNull LocalDateTime endDate) {
}
