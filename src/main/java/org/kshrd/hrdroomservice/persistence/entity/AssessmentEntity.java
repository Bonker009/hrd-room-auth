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
@Table(name = "assessments")
public class AssessmentEntity {

    @Id
    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Column(name = "name")
    private String name;

    @Column(name = "assessment_date")
    private java.time.LocalDate assessmentDate;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
