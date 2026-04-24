package org.kshrd.hrdroomservice.api.dto.auth;

import java.util.List;
import java.util.UUID;

public record AuthMeResponse(
        String subject,
        String username,
        String email,
        String firstName,
        String lastName,
        List<String> roles,
        UUID activeAcademicYearId,
        String academicYearName,
        Integer generation) {}
