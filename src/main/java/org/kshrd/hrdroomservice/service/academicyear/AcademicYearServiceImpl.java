package org.kshrd.hrdroomservice.service.academicyear;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearRequest;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearResponse;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.CourseType;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.mapper.AcademicYearEntityMapper;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.repository.AcademicYearRepository;
import org.kshrd.hrdroomservice.persistence.repository.CourseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AcademicYearServiceImpl implements AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearEntityMapper academicYearMapper;

    @Override
    @Transactional
    public AcademicYearResponse create(AcademicYearRequest request, UUID actorId) {
        if (!StringUtils.hasText(request.name())) {
            throw ApiException.badRequest("Academic year name is required");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("endDate must be after startDate");
        }
        AcademicYearEntity row = academicYearMapper.toNewEntity(request);
        academicYearRepository.save(row);
        return toResponse(row);
    }

    @Override
    @Transactional
    public AcademicYearResponse activate(UUID academicYearId, UUID actorId) {
        AcademicYearEntity target =
                academicYearRepository
                        .findById(academicYearId)
                        .orElseThrow(() -> ApiException.notFound("Academic year not found"));
        if (YearStatus.ACTIVE.name().equals(target.getStatus())) {
            return toResponse(target);
        }
        academicYearRepository.archiveAllActive(actorId);
        target.setStatus(YearStatus.ACTIVE.name());
        academicYearRepository.save(target);
        seedCoursesIfMissing(academicYearId);
        return toResponse(
                academicYearRepository
                        .findById(academicYearId)
                        .orElseThrow(() -> ApiException.notFound("Academic year not found")));
    }

    private void seedCoursesIfMissing(UUID academicYearId) {
        if (courseRepository.countByAcademicYearId(academicYearId) > 0) {
            return;
        }
        CourseEntity basic =
                academicYearMapper.toNewCourseEntity(
                        "BASIC",
                        CourseType.BASIC.name(),
                        "Seeded BASIC course",
                        academicYearId,
                        null);
        courseRepository.save(basic);

        CourseEntity advanced =
                academicYearMapper.toNewCourseEntity(
                        "ADVANCED",
                        CourseType.ADVANCED.name(),
                        "Seeded ADVANCED course",
                        academicYearId,
                        basic.getCourseId());
        courseRepository.save(advanced);
    }

    @Override
    @Transactional
    public AcademicYearResponse archive(UUID academicYearId, UUID actorId) {
        AcademicYearEntity target =
                academicYearRepository
                        .findById(academicYearId)
                        .orElseThrow(() -> ApiException.notFound("Academic year not found"));
        if (!YearStatus.ACTIVE.name().equals(target.getStatus())) {
            throw ApiException.badRequest("Academic year is not active");
        }
        if (academicYearRepository.countByStatus(YearStatus.ACTIVE.name()) == 1) {
            throw ApiException.badRequest(
                    "Cannot archive the only active year without activating another first");
        }
        target.setStatus(YearStatus.ARCHIVED.name());
        return toResponse(academicYearRepository.save(target));
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse getActiveOrThrow() {
        return findActive().orElseThrow(() -> ApiException.notFound("No active academic year"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AcademicYearResponse> findActive() {
        return academicYearRepository
                .findFirstByStatus(YearStatus.ACTIVE.name())
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse getById(UUID id) {
        return academicYearRepository
                .findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> ApiException.notFound("Academic year not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AcademicYearResponse> list(boolean includeArchived, int page, int size) {
        if (includeArchived) {
            return PageResponse.of(
                    academicYearRepository.findAllByOrderByStartDateDesc(
                            PageRequest.of(page, size)),
                    this::toResponse);
        }
        return PageResponse.of(
                academicYearRepository.findByStatusOrderByStartDateDesc(
                        YearStatus.ACTIVE.name(), PageRequest.of(page, size)),
                this::toResponse);
    }

    private AcademicYearResponse toResponse(AcademicYearEntity e) {
        return new AcademicYearResponse(
                e.getAcademicYearId(),
                e.getName(),
                e.getGeneration(),
                e.getStatus(),
                e.getStartDate(),
                e.getEndDate(),
                e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
