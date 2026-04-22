package org.kshrd.hrdroomservice.persistence.entity;

import java.util.UUID;
import lombok.Data;

@Data
public class ClassroomSubjectRow {

    private UUID classroomId;
    private UUID subjectId;
}
