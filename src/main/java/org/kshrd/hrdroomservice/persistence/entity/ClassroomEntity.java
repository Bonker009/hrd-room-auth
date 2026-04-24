package org.kshrd.hrdroomservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "classrooms")
public class ClassroomEntity extends AuditableEntity {

    @Id
    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "class_name")
    private String className;

    @Column(name = "classroom_abbre")
    private String classroomAbbre;

    @Column(name = "description")
    private String description;

    @Column(name = "image")
    private String image;

    @Column(name = "academic_year_id")
    private UUID academicYearId;
}
