package org.kshrd.hrdroomservice.service.academicyear;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearRequest;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearResponse;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.CourseType;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.mapper.AcademicYearMapper;
import org.kshrd.hrdroomservice.persistence.mapper.CourseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AcademicYearServiceImpl implements AcademicYearService {

    private final AcademicYearMapper academicYearMapper;
    private final CourseMapper courseMapper;

    @Override
    @Transactional
    public AcademicYearResponse create(AcademicYearRequest request, UUID actorId) {
        if (!StringUtils.hasText(request.getName())) {
            throw ApiException.badRequest("Academic year name is required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw ApiException.badRequest("endDate must be after startDate");
        }
        AcademicYearEntity row = new AcademicYearEntity();
        row.setAcademicYearId(UUID.randomUUID());
        row.setName(request.getName().trim());
        row.setGeneration(request.getGeneration());
        row.setStatus(YearStatus.ARCHIVED.name());
        row.setStartDate(request.getStartDate());
        row.setEndDate(request.getEndDate());
        row.setVersion(0L);
        row.setCreatedBy(actorId);
        row.setUpdatedBy(actorId);
        row.setCreatedAt(LocalDateTime.now());
        academicYearMapper.insert(row);
        return toResponse(row);
    }

    @Override
    @Transactional
    public AcademicYearResponse activate(UUID academicYearId, UUID actorId) {
        AcademicYearEntity target = academicYearMapper.findById(academicYearId);
        if (target == null) {
            throw ApiException.notFound("Academic year not found");
        }
        if (YearStatus.ACTIVE.name().equals(target.getStatus())) {
            return toResponse(target);
        }
        academicYearMapper.archiveAllActive(actorId);
        academicYearMapper.activate(academicYearId, actorId);
        seedCoursesIfMissing(academicYearId, actorId);
        AcademicYearEntity refreshed = academicYearMapper.findById(academicYearId);
        return toResponse(refreshed);
    }

    private void seedCoursesIfMissing(UUID academicYearId, UUID actorId) {
        if (courseMapper.countByAcademicYearId(academicYearId) > 0) {
            return;
        }
        UUID basicId = UUID.randomUUID();
        CourseEntity basic = new CourseEntity();
        basic.setCourseId(basicId);
        basic.setName("BASIC");
        basic.setType(CourseType.BASIC.name());
        basic.setAcademicYearId(academicYearId);
        basic.setDescription("Seeded BASIC course");
        basic.setIsArchived(false);
        basic.setVersion(0L);
        basic.setCreatedBy(actorId);
        basic.setUpdatedBy(actorId);
        basic.setCreatedAt(LocalDateTime.now());
        courseMapper.insert(basic);

        CourseEntity advanced = new CourseEntity();
        advanced.setCourseId(UUID.randomUUID());
        advanced.setName("ADVANCED");
        advanced.setType(CourseType.ADVANCED.name());
        advanced.setAcademicYearId(academicYearId);
        advanced.setPrerequisiteCourseId(basicId);
        advanced.setDescription("Seeded ADVANCED course");
        advanced.setIsArchived(false);
        advanced.setVersion(0L);
        advanced.setCreatedBy(actorId);
        advanced.setUpdatedBy(actorId);
        advanced.setCreatedAt(LocalDateTime.now());
        courseMapper.insert(advanced);
    }

    @Override
    @Transactional
    public AcademicYearResponse archive(UUID academicYearId, UUID actorId) {
        AcademicYearEntity target = academicYearMapper.findById(academicYearId);
        if (target == null) {
            throw ApiException.notFound("Academic year not found");
        }
        if (!YearStatus.ACTIVE.name().equals(target.getStatus())) {
            throw ApiException.badRequest("Academic year is not active");
        }
        if (academicYearMapper.countActive() == 1) {
            throw ApiException.badRequest("Cannot archive the only active year without activating another first");
        }
        int updated = academicYearMapper.archiveOne(academicYearId, actorId);
        if (updated == 0) {
            throw ApiException.badRequest("Unable to archive academic year");
        }
        return toResponse(academicYearMapper.findById(academicYearId));
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse getActiveOrThrow() {
        return findActive().orElseThrow(() -> ApiException.notFound("No active academic year"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AcademicYearResponse> findActive() {
        AcademicYearEntity active = academicYearMapper.findActive();
        if (active == null) {
            return Optional.empty();
        }
        return Optional.of(toResponse(active));
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse getById(UUID id) {
        AcademicYearEntity row = academicYearMapper.findById(id);
        if (row == null) {
            throw ApiException.notFound("Academic year not found");
        }
        return toResponse(row);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYearResponse> list(boolean includeArchived) {
        return academicYearMapper.findAll(includeArchived).stream().map(this::toResponse).toList();
    }

    private AcademicYearResponse toResponse(AcademicYearEntity e) {
        return AcademicYearResponse.builder()
                .academicYearId(e.getAcademicYearId())
                .name(e.getName())
                .generation(e.getGeneration())
                .status(e.getStatus())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
