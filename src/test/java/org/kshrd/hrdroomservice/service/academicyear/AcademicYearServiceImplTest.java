package org.kshrd.hrdroomservice.service.academicyear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearRequest;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.repository.AcademicYearRepository;
import org.kshrd.hrdroomservice.persistence.repository.CourseRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceImplTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks private AcademicYearServiceImpl service;

    @Test
    void archive_shouldRejectWhenOnlyOneActiveYear() {
        UUID yearId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        AcademicYearEntity active = new AcademicYearEntity();
        active.setAcademicYearId(yearId);
        active.setStatus(YearStatus.ACTIVE.name());

        when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(active));
        when(academicYearRepository.countByStatus(YearStatus.ACTIVE.name())).thenReturn(1);

        ApiException ex = assertThrows(ApiException.class, () -> service.archive(yearId, actorId));
        assertEquals(
                "Cannot archive the only active year without activating another first",
                ex.getMessage());
    }

    @Test
    void create_shouldValidateDateRange() {
        UUID actorId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        AcademicYearRequest request = new AcademicYearRequest("2026", 1, now, now.minusDays(1));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request, actorId));
        assertEquals("endDate must be after startDate", ex.getMessage());
    }

    @Test
    void findActive_shouldMapWhenPresent() {
        AcademicYearEntity active = new AcademicYearEntity();
        active.setAcademicYearId(UUID.randomUUID());
        active.setName("AY");
        active.setGeneration(1);
        active.setStatus(YearStatus.ACTIVE.name());

        when(academicYearRepository.findFirstByStatus(YearStatus.ACTIVE.name()))
                .thenReturn(Optional.of(active));

        assertEquals(
                active.getAcademicYearId(), service.findActive().orElseThrow().academicYearId());
        verify(academicYearRepository).findFirstByStatus(YearStatus.ACTIVE.name());
    }
}
