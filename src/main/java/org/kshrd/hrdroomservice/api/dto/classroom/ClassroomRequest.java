package org.kshrd.hrdroomservice.api.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;

@Data
public class ClassroomRequest {

    @NotBlank private String className;

    @NotBlank private String classroomAbbre;

    private String description;

    private String image;

    /** Optional initial subject link when creating. */
    private UUID subjectId;
}
