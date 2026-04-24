package org.kshrd.hrdroomservice.service.account;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.repository.AcademicYearRepository;
import org.kshrd.hrdroomservice.persistence.repository.ClassroomStudentRepository;
import org.kshrd.hrdroomservice.persistence.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActiveAcademicContextService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final AcademicYearRepository academicYearRepository;

    @Transactional(readOnly = true)
    public Optional<ActiveAcademicContext> resolveForStudent(UUID studentId) {
        UUID yearId =
                firstNonNull(
                        enrollmentRepository.findActiveAcademicYearsForStudent(studentId).stream()
                                .findFirst()
                                .orElse(null),
                        classroomStudentRepository
                                .findActiveAcademicYearsForStudent(studentId)
                                .stream()
                                .findFirst()
                                .orElse(null));
        if (yearId == null) {
            return Optional.empty();
        }
        AcademicYearEntity year = academicYearRepository.findById(yearId).orElse(null);
        if (year == null) {
            return Optional.empty();
        }
        return Optional.of(
                new ActiveAcademicContext(
                        year.getAcademicYearId(), year.getName(), year.getGeneration()));
    }

    private static UUID firstNonNull(UUID a, UUID b) {
        if (a != null) {
            return a;
        }
        return b;
    }
}
