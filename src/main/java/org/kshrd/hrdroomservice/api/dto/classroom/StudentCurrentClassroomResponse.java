package org.kshrd.hrdroomservice.api.dto.classroom;

import java.util.List;
import java.util.UUID;

public record StudentCurrentClassroomResponse(
        ClassroomResponse classroom,
        UUID academicYearId,
        String academicYearName,
        Integer generation,
        List<CourseSummary> courses) {}
