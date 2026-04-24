package org.kshrd.hrdroomservice.api.dto.classroom;

import java.util.UUID;

public record ClassroomStudentCountResponse(UUID classroomId, long studentCount) {}
