package org.kshrd.hrdroomservice.mapper;

import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomRequest;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.ClassroomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        imports = {UUID.class})
public interface ClassroomEntityMapper {

    @Mapping(target = "classroomId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "className", expression = "java(request.className().trim())")
    @Mapping(target = "classroomAbbre", expression = "java(request.classroomAbbre().trim())")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "image", source = "request.image")
    @Mapping(target = "academicYearId", source = "activeYear.academicYearId")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ClassroomEntity toNewEntity(ClassroomRequest request, AcademicYearEntity activeYear);
}
