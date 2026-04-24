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
@Table(name = "classroom_teacher")
public class ClassroomTeacherEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
}
