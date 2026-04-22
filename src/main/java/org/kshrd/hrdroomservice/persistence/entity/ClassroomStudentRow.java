package org.kshrd.hrdroomservice.persistence.entity;

import java.util.UUID;
import lombok.Data;

@Data
public class ClassroomStudentRow {

    private UUID classroomId;
    private UUID studentId;
}
