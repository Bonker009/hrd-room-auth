package org.kshrd.hrdroomservice.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicYearRepository extends JpaRepository<AcademicYearEntity, UUID> {

    Optional<AcademicYearEntity> findFirstByStatus(String status);

    int countByStatus(String status);

    List<AcademicYearEntity> findByStatusOrderByStartDateDesc(String status);

    Page<AcademicYearEntity> findByStatusOrderByStartDateDesc(String status, Pageable pageable);

    List<AcademicYearEntity> findAllByOrderByStartDateDesc();

    Page<AcademicYearEntity> findAllByOrderByStartDateDesc(Pageable pageable);

    @Modifying
    @Query(
            """
            update AcademicYearEntity y
            set y.status = 'ARCHIVED',
                y.updatedAt = CURRENT_TIMESTAMP,
                y.updatedBy = :updatedBy
            where y.status = 'ACTIVE'
            """)
    int archiveAllActive(@Param("updatedBy") UUID updatedBy);
}
