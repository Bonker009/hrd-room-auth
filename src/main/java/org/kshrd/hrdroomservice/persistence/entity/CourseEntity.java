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
@Table(name = "courses")
public class CourseEntity extends VersionedAuditableEntity {

    @Id
    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(name = "prerequisite_course_id")
    private UUID prerequisiteCourseId;

    @Column(name = "description")
    private String description;

    @Column(name = "is_archived")
    private Boolean isArchived;
}
