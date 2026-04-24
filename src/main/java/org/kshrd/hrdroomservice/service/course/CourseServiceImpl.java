package org.kshrd.hrdroomservice.service.course;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.course.CourseResponse;
import org.kshrd.hrdroomservice.api.dto.course.CourseUpdateRequest;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.mapper.AcademicYearMapper;
import org.kshrd.hrdroomservice.persistence.mapper.CourseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final AcademicYearMapper academicYearMapper;

    @Override
    @Transactional
    public CourseResponse update(UUID courseId, CourseUpdateRequest request, UUID actorId) {
        CourseEntity course = courseMapper.findById(courseId);
        if (course == null) {
            throw ApiException.notFound("Course not found");
        }
        AcademicYearEntity year = academicYearMapper.findById(course.getAcademicYearId());
        if (year == null || !YearStatus.ACTIVE.name().equals(year.getStatus())) {
            throw ApiException.badRequest("Course must belong to an active academic year");
        }
        long version = course.getVersion() == null ? 0L : course.getVersion();
        int updated =
                courseMapper.updateNameDescription(
                        courseId, request.name(), request.description(), version, actorId);
        if (updated == 0) {
            throw ApiException.conflict("Course was modified concurrently");
        }
        return toResponse(courseMapper.findById(courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> list(
            String type, UUID academicYearId, boolean includeArchived, boolean onlyActiveYears) {
        return courseMapper.search(type, academicYearId, includeArchived, onlyActiveYears).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getById(UUID courseId) {
        CourseEntity course = courseMapper.findById(courseId);
        if (course == null) {
            throw ApiException.notFound("Course not found");
        }
        return toResponse(course);
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
