package org.kshrd.hrdroomservice.persistence.mapper;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomEntity;

@Mapper
public interface ClassroomMapper {

    @Insert(
            """
            INSERT INTO classrooms (
                classroom_id, class_name, classroom_abbre, description, image, academic_year_id,
                created_by, updated_by, created_at, updated_at
            ) VALUES (
                #{classroomId}, #{className}, #{classroomAbbre}, #{description}, #{image}, #{academicYearId},
                #{createdBy}, #{updatedBy}, COALESCE(#{createdAt}, now()), #{updatedAt}
            )
            """)
    int insert(ClassroomEntity row);

    @Update(
            """
            UPDATE classrooms
            SET class_name = #{className},
                classroom_abbre = #{classroomAbbre},
                description = #{description},
                image = #{image},
                updated_at = now(),
                updated_by = #{updatedBy}
            WHERE classroom_id = #{classroomId}
            """)
    int update(ClassroomEntity row);

    @Delete("DELETE FROM classrooms WHERE classroom_id = #{id}")
    int delete(@Param("id") UUID id);

    @Select("SELECT * FROM classrooms WHERE classroom_id = #{id}")
    ClassroomEntity findById(@Param("id") UUID id);

    @Select(
            "SELECT * FROM classrooms ORDER BY created_at DESC OFFSET #{offset} LIMIT #{limit}")
    List<ClassroomEntity> pageAll(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM classrooms")
    long countAll();

    @Select(
            """
            SELECT DISTINCT c.*
            FROM classrooms c
            INNER JOIN classroom_teacher ct ON ct.classroom_id = c.classroom_id
            WHERE ct.teacher_id = #{teacherId}
            ORDER BY c.created_at DESC
            OFFSET #{offset} LIMIT #{limit}
            """)
    List<ClassroomEntity> pageForTeacher(
            @Param("teacherId") UUID teacherId, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            """
            SELECT COUNT(DISTINCT c.classroom_id)
            FROM classrooms c
            INNER JOIN classroom_teacher ct ON ct.classroom_id = c.classroom_id
            WHERE ct.teacher_id = #{teacherId}
            """)
    long countForTeacher(@Param("teacherId") UUID teacherId);

    @Select(
            """
            SELECT c.*
            FROM classrooms c
            INNER JOIN classroom_subject cs ON cs.classroom_id = c.classroom_id
            WHERE cs.subject_id = #{subjectId}
            ORDER BY c.created_at DESC
            """)
    List<ClassroomEntity> findBySubject(@Param("subjectId") UUID subjectId);

    @Update(
            """
            UPDATE classrooms
            SET image = #{image},
                updated_at = now(),
                updated_by = #{updatedBy}
            WHERE classroom_id = #{id}
            """)
    int updateImage(@Param("id") UUID id, @Param("image") String image, @Param("updatedBy") UUID updatedBy);

    @Select("SELECT * FROM classrooms ORDER BY created_at DESC")
    List<ClassroomEntity> findAll();
}
