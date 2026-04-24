package org.kshrd.hrdroomservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "classroom_subject")
public class ClassroomSubjectEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "subject_id")
    private UUID subjectId;
}
