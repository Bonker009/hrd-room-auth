package org.kshrd.hrdroomservice.persistence.mapper;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentEntity;

@Mapper
public interface EnrollmentMapper {

    @Insert(
            """
            INSERT INTO enrollments (
                enrollment_id, student_id, course_id, academic_year_id, enrolled_at, grade, is_passed, completed_at,
                is_terminated, terminated_at, termination_reason, version, created_by, updated_by, created_at, updated_at
            ) VALUES (
                #{enrollmentId}, #{studentId}, #{courseId}, #{academicYearId}, COALESCE(#{enrolledAt}, now()), #{grade},
                COALESCE(#{isPassed}, false), #{completedAt},
                COALESCE(#{isTerminated}, false), #{terminatedAt}, #{terminationReason},
                COALESCE(#{version}, 0), #{createdBy}, #{updatedBy}, COALESCE(#{createdAt}, now()), #{updatedAt}
            )
            """)
    int insert(EnrollmentEntity row);

    @Select(
            "SELECT COUNT(*) FROM enrollments WHERE student_id = #{studentId} AND course_id = #{courseId}")
    int countByStudentAndCourse(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    @Select("SELECT * FROM enrollments WHERE enrollment_id = #{id}")
    EnrollmentEntity findById(@Param("id") UUID id);

    @Select(
            "SELECT * FROM enrollments WHERE student_id = #{studentId} AND course_id = #{courseId}")
    EnrollmentEntity findByStudentAndCourse(
            @Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    @Select(
            """
            <script>
            SELECT e.*
            FROM enrollments e
            JOIN courses c ON c.course_id = e.course_id
            JOIN academic_years y ON y.academic_year_id = c.academic_year_id
            <where>
              <if test="!includeArchived">AND y.status = 'ACTIVE'</if>
            </where>
            ORDER BY e.enrolled_at DESC
            </script>
            """)
    List<EnrollmentEntity> findAll(@Param("includeArchived") boolean includeArchived);

    @Select(
            """
            <script>
            SELECT e.*
            FROM enrollments e
            JOIN courses c ON c.course_id = e.course_id
            JOIN academic_years y ON y.academic_year_id = c.academic_year_id
            WHERE e.student_id = #{studentId}
            <if test="!includeArchived">AND y.status = 'ACTIVE'</if>
            ORDER BY e.enrolled_at DESC
            </script>
            """)
    List<EnrollmentEntity> findByStudent(
            @Param("studentId") UUID studentId, @Param("includeArchived") boolean includeArchived);

    @Select("SELECT * FROM enrollments WHERE course_id = #{courseId} ORDER BY enrolled_at DESC")
    List<EnrollmentEntity> findByCourse(@Param("courseId") UUID courseId);

    @Update(
            """
            UPDATE enrollments
            SET is_terminated = true,
                terminated_at = now(),
                termination_reason = #{reason},
                updated_at = now(),
                version = COALESCE(version, 0) + 1,
                updated_by = #{updatedBy}
            WHERE enrollment_id = #{id} AND COALESCE(version, 0) = #{version}
            """)
    int terminate(
            @Param("id") UUID id,
            @Param("reason") String reason,
            @Param("version") long version,
            @Param("updatedBy") UUID updatedBy);

    @Update(
            """
            UPDATE enrollments
            SET is_terminated = false,
                terminated_at = null,
                termination_reason = null,
                updated_at = now(),
                version = COALESCE(version, 0) + 1,
                updated_by = #{updatedBy}
            WHERE enrollment_id = #{id} AND COALESCE(version, 0) = #{version}
            """)
    int reactivate(@Param("id") UUID id, @Param("version") long version, @Param("updatedBy") UUID updatedBy);

    @Update(
            """
            UPDATE enrollments
            SET is_passed = true,
                completed_at = now(),
                updated_at = now(),
                version = COALESCE(version, 0) + 1,
                updated_by = #{updatedBy}
            WHERE enrollment_id = #{id} AND COALESCE(version, 0) = #{version}
            """)
    int markPassed(@Param("id") UUID id, @Param("version") long version, @Param("updatedBy") UUID updatedBy);

    @Select(
            """
            SELECT DISTINCT c.academic_year_id
            FROM enrollments e
            JOIN courses c ON c.course_id = e.course_id
            JOIN academic_years y ON y.academic_year_id = c.academic_year_id
            WHERE e.student_id = #{studentId}
              AND y.status = 'ACTIVE'
              AND COALESCE(e.is_terminated, false) = false
            LIMIT 1
            """)
    UUID findActiveAcademicYearForStudentFromEnrollments(@Param("studentId") UUID studentId);
}
