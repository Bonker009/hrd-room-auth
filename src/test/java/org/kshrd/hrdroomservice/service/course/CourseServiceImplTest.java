package org.kshrd.hrdroomservice.service.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kshrd.hrdroomservice.api.dto.course.CourseResponse;
import org.kshrd.hrdroomservice.api.dto.course.CourseUpdateRequest;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.domain.YearStatus;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.entity.CourseEntity;
import org.kshrd.hrdroomservice.persistence.repository.AcademicYearRepository;
import org.kshrd.hrdroomservice.persistence.repository.CourseRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock private CourseRepository courseRepository;
    @Mock private AcademicYearRepository academicYearRepository;

    @InjectMocks private CourseServiceImpl courseService;

    @Test
    void update_shouldThrowConflict_whenOptimisticLockingFails() {
        UUID courseId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        CourseEntity existingCourse = new CourseEntity();
        existingCourse.setCourseId(courseId);
        existingCourse.setAcademicYearId(yearId);

        AcademicYearEntity activeYear = new AcademicYearEntity();
        activeYear.setAcademicYearId(yearId);
        activeYear.setStatus(YearStatus.ACTIVE.name());

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(activeYear));
        when(courseRepository.saveAndFlush(existingCourse))
                .thenThrow(
                        new ObjectOptimisticLockingFailureException(CourseEntity.class, courseId));

        ApiException ex =
                assertThrows(
                        ApiException.class,
                        () ->
                                courseService.update(
                                        courseId,
                                        new CourseUpdateRequest("Updated", "Description"),
                                        actorId));

        assertEquals("Course was modified concurrently", ex.getMessage());
        assertEquals("CONFLICT", ex.getErrorCode());
    }

    @Test
    void list_shouldUseCreatedAtDescendingSort_andMapResults() {
        UUID courseId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();

        CourseEntity row = new CourseEntity();
        row.setCourseId(courseId);
        row.setCode("GIC");
        row.setName("Gen AI");
        row.setType("CORE");
        row.setAcademicYearId(yearId);
        row.setIsArchived(false);
        row.setVersion(0L);
        row.setCreatedAt(LocalDateTime.now());

        when(courseRepository.findAll(specificationAny(), any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(
                                List.of(row),
                                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                                1));

        PageResponse<CourseResponse> result =
                courseService.list("CORE", yearId, false, true, 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(courseId, result.content().getFirst().courseId());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).findAll(specificationAny(), pageableCaptor.capture());
        assertEquals(
                Sort.Direction.DESC,
                pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void update_shouldRejectWhenAcademicYearIsNotActive() {
        UUID courseId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        CourseEntity existingCourse = new CourseEntity();
        existingCourse.setCourseId(courseId);
        existingCourse.setAcademicYearId(yearId);

        AcademicYearEntity archivedYear = new AcademicYearEntity();
        archivedYear.setAcademicYearId(yearId);
        archivedYear.setStatus(YearStatus.ARCHIVED.name());

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(archivedYear));

        ApiException ex =
                assertThrows(
                        ApiException.class,
                        () ->
                                courseService.update(
                                        courseId,
                                        new CourseUpdateRequest("Updated", "Description"),
                                        actorId));

        assertEquals("Course must belong to an active academic year", ex.getMessage());
        assertEquals("BAD_REQUEST", ex.getErrorCode());
        verify(courseRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @SuppressWarnings("unchecked")
    private static Specification<CourseEntity> specificationAny() {
        return (Specification<CourseEntity>) any(Specification.class);
    }
}
