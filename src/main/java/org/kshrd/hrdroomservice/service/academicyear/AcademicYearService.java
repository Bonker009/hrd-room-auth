package org.kshrd.hrdroomservice.service.academicyear;

import java.util.Optional;
import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearRequest;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearResponse;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;

public interface AcademicYearService {

    AcademicYearResponse create(AcademicYearRequest request, UUID actorId);

    AcademicYearResponse activate(UUID academicYearId, UUID actorId);

    AcademicYearResponse archive(UUID academicYearId, UUID actorId);

    AcademicYearResponse getActiveOrThrow();

    Optional<AcademicYearResponse> findActive();

    AcademicYearResponse getById(UUID id);

    PageResponse<AcademicYearResponse> list(boolean includeArchived, int page, int size);
}
