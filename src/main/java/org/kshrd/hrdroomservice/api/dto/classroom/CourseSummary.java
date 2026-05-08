package org.kshrd.hrdroomservice.api.dto.classroom;

import java.util.UUID;

public record CourseSummary(UUID courseId, String code, String name, String type) {}
