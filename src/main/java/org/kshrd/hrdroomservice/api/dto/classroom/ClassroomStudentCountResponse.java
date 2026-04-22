package org.kshrd.hrdroomservice.api.dto.classroom;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomStudentCountResponse {

    private UUID classroomId;
    private long studentCount;
}
