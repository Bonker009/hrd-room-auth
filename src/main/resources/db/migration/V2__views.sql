
CREATE VIEW v_enrollment_detail AS
SELECT
    e.enrollment_id,
    e.student_id,
    e.course_id,
    c.name          AS course_name,
    c.type          AS course_type,
    e.academic_year_id,
    y.name          AS academic_year_name,
    y.generation    AS generation,
    y.status        AS academic_year_status,
    e.enrolled_at,
    e.is_passed,
    e.is_terminated,
    e.terminated_at,
    e.termination_reason,
    e.version
FROM enrollments e
LEFT JOIN courses         c ON c.course_id        = e.course_id
LEFT JOIN academic_years  y ON y.academic_year_id = e.academic_year_id;
