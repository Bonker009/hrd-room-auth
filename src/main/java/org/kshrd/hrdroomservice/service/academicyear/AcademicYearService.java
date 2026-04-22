package org.kshrd.hrdroomservice.service.academicyear;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearRequest;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearResponse;

public interface AcademicYearService {

    AcademicYearResponse create(AcademicYearRequest request, UUID actorId);

    AcademicYearResponse activate(UUID academicYearId, UUID actorId);

    AcademicYearResponse archive(UUID academicYearId, UUID actorId);

    AcademicYearResponse getActiveOrThrow();

    Optional<AcademicYearResponse> findActive();

    AcademicYearResponse getById(UUID id);

    List<AcademicYearResponse> list(boolean includeArchived);
}
