package org.kshrd.hrdroomservice.api.dto.enrollment;

import java.util.List;

public record EnrollmentByCourseTypeResponse(
        List<EnrollmentResponse> basic, List<EnrollmentResponse> advanced) {}
