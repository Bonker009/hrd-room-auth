package org.kshrd.hrdroomservice.persistence.mapper;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomStudentCountResponse;
import org.kshrd.hrdroomservice.persistence.entity.AssessmentEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomStudentRow;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomSubjectRow;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomTeacherRow;

@Mapper
public interface ClassroomRelationMapper {

    @Insert(
            """
            INSERT INTO classroom_teacher (id, classroom_id, teacher_id, assigned_at)
            SELECT gen_random_uuid(), #{classroomId}, #{teacherId}, now()
            WHERE NOT EXISTS (
                SELECT 1 FROM classroom_teacher
                WHERE classroom_id = #{classroomId} AND teacher_id = #{teacherId}
            )
            """)
    int insertTeacher(@Param("classroomId") UUID classroomId, @Param("teacherId") UUID teacherId);

    @Delete(
            "DELETE FROM classroom_teacher WHERE classroom_id = #{classroomId} AND teacher_id = #{teacherId}")
    int deleteTeacher(@Param("classroomId") UUID classroomId, @Param("teacherId") UUID teacherId);

    @Select(
            "SELECT teacher_id FROM classroom_teacher WHERE classroom_id = #{classroomId} ORDER BY assigned_at")
    List<UUID> listTeacherIds(@Param("classroomId") UUID classroomId);

    @Insert(
            """
            INSERT INTO classroom_student (id, classroom_id, student_id, assigned_at)
            SELECT gen_random_uuid(), #{classroomId}, #{studentId}, now()
            WHERE NOT EXISTS (
                SELECT 1 FROM classroom_student
                WHERE classroom_id = #{classroomId} AND student_id = #{studentId}
            )
            """)
    int insertStudent(@Param("classroomId") UUID classroomId, @Param("studentId") UUID studentId);

    @Delete(
            "DELETE FROM classroom_student WHERE classroom_id = #{classroomId} AND student_id = #{studentId}")
    int deleteStudent(@Param("classroomId") UUID classroomId, @Param("studentId") UUID studentId);

    @Select(
            "SELECT student_id FROM classroom_student WHERE classroom_id = #{classroomId} ORDER BY assigned_at")
    List<UUID> listStudentIds(@Param("classroomId") UUID classroomId);

    @Insert(
            """
            INSERT INTO classroom_subject (id, classroom_id, subject_id)
            SELECT gen_random_uuid(), #{classroomId}, #{subjectId}
            WHERE NOT EXISTS (
                SELECT 1 FROM classroom_subject
                WHERE classroom_id = #{classroomId} AND subject_id = #{subjectId}
            )
            """)
    int insertSubject(@Param("classroomId") UUID classroomId, @Param("subjectId") UUID subjectId);

    @Delete(
            "DELETE FROM classroom_subject WHERE classroom_id = #{classroomId} AND subject_id = #{subjectId}")
    int deleteSubject(@Param("classroomId") UUID classroomId, @Param("subjectId") UUID subjectId);

    @Select("SELECT subject_id FROM classroom_subject WHERE classroom_id = #{classroomId}")
    List<UUID> listSubjectIds(@Param("classroomId") UUID classroomId);

    @Insert(
            """
            INSERT INTO assessment_classroom (id, assessment_id, classroom_id, assigned_at)
            SELECT gen_random_uuid(), #{assessmentId}, #{classroomId}, now()
            WHERE NOT EXISTS (
                SELECT 1 FROM assessment_classroom
                WHERE assessment_id = #{assessmentId} AND classroom_id = #{classroomId}
            )
            """)
    int insertAssessment(@Param("classroomId") UUID classroomId, @Param("assessmentId") UUID assessmentId);

    @Delete(
            """
            DELETE FROM assessment_classroom
            WHERE classroom_id = #{classroomId} AND assessment_id = #{assessmentId}
            """)
    int deleteAssessment(@Param("classroomId") UUID classroomId, @Param("assessmentId") UUID assessmentId);

    @Delete(
            """
            <script>
            DELETE FROM assessment_classroom
            WHERE classroom_id = #{classroomId}
            <if test="assessmentIds != null and assessmentIds.size() &gt; 0">
              AND assessment_id IN
              <foreach collection="assessmentIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            </if>
            </script>
            """)
    int deleteAssessments(
            @Param("classroomId") UUID classroomId, @Param("assessmentIds") List<UUID> assessmentIds);

    @Select(
            """
            SELECT a.assessment_id, a.name, a.assessment_date, a.academic_year_id, a.created_at
            FROM assessment_classroom ac
            JOIN assessments a ON a.assessment_id = ac.assessment_id
            WHERE ac.classroom_id = #{classroomId}
            ORDER BY ac.assigned_at DESC
            """)
    List<AssessmentEntity> listAssessments(@Param("classroomId") UUID classroomId);

    @Select(
            """
            SELECT classroom_id AS classroomId, COUNT(*)::bigint AS studentCount
            FROM classroom_student
            GROUP BY classroom_id
            """)
    List<ClassroomStudentCountResponse> studentCounts();

    @Delete("DELETE FROM classroom_teacher WHERE classroom_id = #{classroomId}")
    int deleteAllTeachers(@Param("classroomId") UUID classroomId);

    @Delete("DELETE FROM classroom_student WHERE classroom_id = #{classroomId}")
    int deleteAllStudents(@Param("classroomId") UUID classroomId);

    @Delete("DELETE FROM classroom_subject WHERE classroom_id = #{classroomId}")
    int deleteAllSubjects(@Param("classroomId") UUID classroomId);

    @Delete("DELETE FROM assessment_classroom WHERE classroom_id = #{classroomId}")
    int deleteAllAssessments(@Param("classroomId") UUID classroomId);

    @Select(
            """
            SELECT classroom_id AS classroomId, teacher_id AS teacherId
            FROM classroom_teacher
            """)
    List<ClassroomTeacherRow> listAllTeacherAssignments();

    @Select(
            """
            SELECT classroom_id AS classroomId, subject_id AS subjectId
            FROM classroom_subject
            """)
    List<ClassroomSubjectRow> listAllSubjectAssignments();

    @Select(
            """
            SELECT classroom_id AS classroomId, student_id AS studentId
            FROM classroom_student
            """)
    List<ClassroomStudentRow> listAllStudentAssignments();

    @Select(
            """
            SELECT c.academic_year_id
            FROM classroom_student cs
            JOIN classrooms c ON c.classroom_id = cs.classroom_id
            JOIN academic_years y ON y.academic_year_id = c.academic_year_id
            WHERE cs.student_id = #{studentId}
              AND y.status = 'ACTIVE'
            LIMIT 1
            """)
    UUID findActiveAcademicYearForStudentFromClassrooms(@Param("studentId") UUID studentId);
}
