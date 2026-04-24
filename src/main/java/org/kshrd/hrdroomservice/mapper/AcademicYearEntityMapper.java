package org.kshrd.hrdroomservice.mapper;

import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearRequest;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        imports = {UUID.class, YearStatus.class})
public interface AcademicYearEntityMapper {

    @Mapping(target = "academicYearId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "generation", source = "request.generation")
    @Mapping(target = "status", expression = "java(YearStatus.ARCHIVED.name())")
    @Mapping(target = "startDate", source = "request.startDate")
    @Mapping(target = "endDate", source = "request.endDate")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AcademicYearEntity toNewEntity(AcademicYearRequest request);

    @Mapping(target = "courseId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "academicYearId", source = "academicYearId")
    @Mapping(target = "prerequisiteCourseId", source = "prerequisiteCourseId")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "isArchived", expression = "java(false)")
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CourseEntity toNewCourseEntity(
            String name,
            String type,
            String description,
            UUID academicYearId,
            UUID prerequisiteCourseId);
}
