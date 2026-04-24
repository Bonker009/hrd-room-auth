package org.kshrd.hrdroomservice.service.course;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.course.CourseResponse;
import org.kshrd.hrdroomservice.api.dto.course.CourseUpdateRequest;

public interface CourseService {

    CourseResponse update(UUID courseId, CourseUpdateRequest request, UUID actorId);

    List<CourseResponse> list(
            String type, UUID academicYearId, boolean includeArchived, boolean onlyActiveYears);

    CourseResponse getById(UUID courseId);
}
