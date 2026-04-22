package org.kshrd.hrdroomservice.api.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AcademicYearRequest {

    @NotBlank private String name;

    private Integer generation;

    @NotNull private LocalDateTime startDate;

    @NotNull private LocalDateTime endDate;
}
