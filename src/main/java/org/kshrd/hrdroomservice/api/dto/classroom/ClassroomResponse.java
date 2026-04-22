package org.kshrd.hrdroomservice.api.dto.classroom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomResponse {

    private UUID classroomId;
    private String className;
    private String classroomAbbre;
    private String description;
    private String image;
    private UUID academicYearId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<UUID> teacherIds;
    private List<UUID> studentIds;
    private List<UUID> subjectIds;
}
