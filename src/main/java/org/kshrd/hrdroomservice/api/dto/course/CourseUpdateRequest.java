package org.kshrd.hrdroomservice.api.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseUpdateRequest {

    @NotBlank private String name;

    private String description;
}
