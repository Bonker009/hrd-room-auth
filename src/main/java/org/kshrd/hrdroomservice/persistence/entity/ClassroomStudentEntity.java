package org.kshrd.hrdroomservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "classroom_student")
public class ClassroomStudentEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
}
