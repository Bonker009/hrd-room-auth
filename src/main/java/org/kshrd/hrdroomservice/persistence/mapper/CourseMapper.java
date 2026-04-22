package org.kshrd.hrdroomservice.persistence.mapper;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;

@Mapper
public interface CourseMapper {

    @Insert(
            """
            INSERT INTO courses (
                course_id, code, name, type, academic_year_id, prerequisite_course_id, description,
                is_archived, version, created_by, updated_by, created_at, updated_at
            ) VALUES (
                #{courseId}, #{code}, #{name}, #{type}, #{academicYearId}, #{prerequisiteCourseId}, #{description},
                COALESCE(#{isArchived}, false), COALESCE(#{version}, 0), #{createdBy}, #{updatedBy},
                COALESCE(#{createdAt}, now()), #{updatedAt}
            )
            """)
    int insert(CourseEntity row);

    @Select("SELECT c.* FROM courses c WHERE c.course_id = #{id}")
    CourseEntity findById(@Param("id") UUID id);

    @Select("SELECT COUNT(*) FROM courses WHERE academic_year_id = #{academicYearId}")
    int countByAcademicYearId(@Param("academicYearId") UUID academicYearId);

    @Update(
            """
            UPDATE courses
            SET name = #{name},
                description = #{description},
                updated_at = now(),
                version = COALESCE(version, 0) + 1,
                updated_by = #{updatedBy}
            WHERE course_id = #{id} AND COALESCE(version, 0) = #{version}
            """)
    int updateNameDescription(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("version") long version,
            @Param("updatedBy") UUID updatedBy);

    @Select(
            """
            <script>
            SELECT c.*
            FROM courses c
            LEFT JOIN academic_years y ON y.academic_year_id = c.academic_year_id
            <where>
              <if test="type != null and type != ''">AND c.type = #{type}</if>
              <if test="academicYearId != null">AND c.academic_year_id = #{academicYearId}</if>
              <if test="!includeArchived">AND c.is_archived = false</if>
              <if test="onlyActiveYears">AND y.status = 'ACTIVE'</if>
            </where>
            ORDER BY c.created_at DESC
            </script>
            """)
    List<CourseEntity> search(
            @Param("type") String type,
            @Param("academicYearId") UUID academicYearId,
            @Param("includeArchived") boolean includeArchived,
            @Param("onlyActiveYears") boolean onlyActiveYears);
}
