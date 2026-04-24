package org.kshrd.hrdroomservice.api.dto.course;

import jakarta.validation.constraints.NotBlank;

public record CourseUpdateRequest(@NotBlank String name, String description) {}
