package org.kshrd.hrdroomservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "enrollments")
public class EnrollmentEntity extends VersionedAuditableEntity {

    @Id
    @Column(name = "enrollment_id")
    private UUID enrollmentId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Column(name = "grade")
    private Double grade;

    @Column(name = "is_passed")
    private Boolean isPassed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_terminated")
    private Boolean isTerminated;

    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    @Column(name = "termination_reason")
    private String terminationReason;
}
