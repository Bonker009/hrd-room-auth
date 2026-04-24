package org.kshrd.hrdroomservice.service.course;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.course.CourseResponse;
import org.kshrd.hrdroomservice.api.dto.course.CourseUpdateRequest;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.repository.AcademicYearRepository;
import org.kshrd.hrdroomservice.persistence.repository.CourseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    @Transactional
    public CourseResponse update(UUID courseId, CourseUpdateRequest request, UUID actorId) {
        CourseEntity course =
                courseRepository
                        .findById(courseId)
                        .orElseThrow(() -> ApiException.notFound("Course not found"));
        AcademicYearEntity year =
                academicYearRepository.findById(course.getAcademicYearId()).orElse(null);
        if (year == null || !YearStatus.ACTIVE.name().equals(year.getStatus())) {
            throw ApiException.badRequest("Course must belong to an active academic year");
        }
        course.setName(request.name());
        course.setDescription(request.description());
        try {
            return toResponse(courseRepository.saveAndFlush(course));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw ApiException.conflict("Course was modified concurrently");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> list(
            String type, UUID academicYearId, boolean includeArchived, boolean onlyActiveYears) {
        return courseRepository
                .findAll(
                        withFilters(type, academicYearId, includeArchived, onlyActiveYears),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getById(UUID courseId) {
        return toResponse(
                courseRepository
                        .findById(courseId)
                        .orElseThrow(() -> ApiException.notFound("Course not found")));
    }

    private Specification<CourseEntity> withFilters(
            String type, UUID academicYearId, boolean includeArchived, boolean onlyActiveYears) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (type != null && !type.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }
            if (academicYearId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("academicYearId"), academicYearId));
            }
            if (!includeArchived) {
                predicate =
                        cb.and(
                                predicate,
                                cb.or(
                                        cb.isFalse(root.get("isArchived")),
                                        cb.isNull(root.get("isArchived"))));
            }
            if (onlyActiveYears) {
                var activeYearSubquery = query.subquery(UUID.class);
                var yearRoot = activeYearSubquery.from(AcademicYearEntity.class);
                activeYearSubquery
                        .select(yearRoot.get("academicYearId"))
                        .where(cb.equal(yearRoot.get("status"), YearStatus.ACTIVE.name()));
                predicate = cb.and(predicate, root.get("academicYearId").in(activeYearSubquery));
            }
            return predicate;
        };
    }

    private CourseResponse toResponse(CourseEntity c) {
        return new CourseResponse(
                c.getCourseId(),
                c.getCode(),
                c.getName(),
                c.getType(),
                c.getAcademicYearId(),
                c.getPrerequisiteCourseId(),
                c.getDescription(),
                c.getIsArchived(),
                c.getVersion(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
