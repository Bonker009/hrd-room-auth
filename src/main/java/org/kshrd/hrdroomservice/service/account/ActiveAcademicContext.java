package org.kshrd.hrdroomservice.service.account;

import java.util.UUID;

public record ActiveAcademicContext(UUID academicYearId, String academicYearName, Integer generation) {}
