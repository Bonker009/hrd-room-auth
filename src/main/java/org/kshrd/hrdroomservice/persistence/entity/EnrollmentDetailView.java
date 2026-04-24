package org.kshrd.hrdroomservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Getter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "v_enrollment_detail")
public class EnrollmentDetailView {

    @Id
    @Column(name = "enrollment_id")
    private UUID enrollmentId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "course_type")
    private String courseType;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(name = "academic_year_name")
    private String academicYearName;

    @Column(name = "generation")
    private Integer generation;

    @Column(name = "academic_year_status")
    private String academicYearStatus;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Column(name = "is_passed")
    private Boolean isPassed;

    @Column(name = "is_terminated")
    private Boolean isTerminated;

    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    @Column(name = "termination_reason")
    private String terminationReason;

    @Column(name = "version")
    private Long version;
}
