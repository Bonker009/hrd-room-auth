package org.kshrd.hrdroomservice.api.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ClassroomRequest(
        @NotBlank String className,
        @NotBlank String classroomAbbre,
        String description,
        String image,
        /** Optional initial subject link when creating. */
        UUID subjectId) {}
