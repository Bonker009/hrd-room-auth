package org.kshrd.hrdroomservice.service.course;

import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.course.CourseResponse;
import org.kshrd.hrdroomservice.api.dto.course.CourseUpdateRequest;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;

public interface CourseService {

    CourseResponse update(UUID courseId, CourseUpdateRequest request, UUID actorId);

    PageResponse<CourseResponse> list(
            String type,
            UUID academicYearId,
            boolean includeArchived,
            boolean onlyActiveYears,
            int page,
            int size);

    CourseResponse getById(UUID courseId);
}
