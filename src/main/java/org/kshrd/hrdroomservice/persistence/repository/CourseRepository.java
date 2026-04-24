package org.kshrd.hrdroomservice.persistence.repository;

import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository
        extends JpaRepository<CourseEntity, UUID>, JpaSpecificationExecutor<CourseEntity> {

    int countByAcademicYearId(UUID academicYearId);
}
