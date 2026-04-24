package org.kshrd.hrdroomservice.persistence.mapper;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;

@Mapper
public interface AcademicYearMapper {

    @Insert(
            """
            INSERT INTO academic_years (
                academic_year_id, name, generation, status, start_date, end_date, version, created_by, updated_by, created_at, updated_at
            ) VALUES (
                #{academicYearId}, #{name}, #{generation}, #{status}, #{startDate}, #{endDate},
                COALESCE(#{version}, 0), #{createdBy}, #{updatedBy}, COALESCE(#{createdAt}, now()), #{updatedAt}
            )
            """)
    int insert(AcademicYearEntity row);

    @Select("SELECT * FROM academic_years WHERE academic_year_id = #{id}")
    AcademicYearEntity findById(@Param("id") UUID id);

    @Select("SELECT * FROM academic_years WHERE status = 'ACTIVE' LIMIT 1")
    AcademicYearEntity findActive();

    @Select("SELECT COUNT(*) FROM academic_years WHERE status = 'ACTIVE'")
    int countActive();

    @Select(
            """
            SELECT * FROM academic_years
            WHERE (#{includeArchived} = TRUE OR status = 'ACTIVE')
            ORDER BY start_date DESC
            """)
    List<AcademicYearEntity> findAll(@Param("includeArchived") boolean includeArchived);

    @Update(
            """
            UPDATE academic_years
            SET status = 'ARCHIVED',
                updated_at = now(),
                version = COALESCE(version, 0) + 1,
                updated_by = #{updatedBy}
            WHERE status = 'ACTIVE'
            """)
    int archiveAllActive(@Param("updatedBy") UUID updatedBy);

    @Update(
            """
            UPDATE academic_years
            SET status = 'ACTIVE',
                updated_at = now(),
                version = COALESCE(version, 0) + 1,
                updated_by = #{updatedBy}
            WHERE academic_year_id = #{id}
            """)
    int activate(@Param("id") UUID id, @Param("updatedBy") UUID updatedBy);

    @Update(
            """
            UPDATE academic_years
            SET status = 'ARCHIVED',
                updated_at = now(),
                version = COALESCE(version, 0) + 1,
                updated_by = #{updatedBy}
            WHERE academic_year_id = #{id} AND status = 'ACTIVE'
            """)
    int archiveOne(@Param("id") UUID id, @Param("updatedBy") UUID updatedBy);
}
