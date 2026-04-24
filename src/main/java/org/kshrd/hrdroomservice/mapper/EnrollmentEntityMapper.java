package org.kshrd.hrdroomservice.mapper;

import java.time.LocalDateTime;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.entity.EnrollmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        imports = {UUID.class, LocalDateTime.class})
public interface EnrollmentEntityMapper {

    @Mapping(target = "enrollmentId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "academicYearId", source = "course.academicYearId")
    @Mapping(target = "enrolledAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "isPassed", expression = "java(false)")
    @Mapping(target = "isTerminated", expression = "java(false)")
    @Mapping(target = "grade", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "terminatedAt", ignore = true)
    @Mapping(target = "terminationReason", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EnrollmentEntity toNewEntity(UUID studentId, UUID courseId, CourseEntity course);
}
