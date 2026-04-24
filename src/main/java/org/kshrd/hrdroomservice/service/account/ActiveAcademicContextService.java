package org.kshrd.hrdroomservice.service.account;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.persistence.entity.AcademicYearEntity;
import org.kshrd.hrdroomservice.persistence.mapper.AcademicYearMapper;
import org.kshrd.hrdroomservice.persistence.mapper.ClassroomRelationMapper;
import org.kshrd.hrdroomservice.persistence.mapper.EnrollmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActiveAcademicContextService {

    private final EnrollmentMapper enrollmentMapper;
    private final ClassroomRelationMapper classroomRelationMapper;
    private final AcademicYearMapper academicYearMapper;

    @Transactional(readOnly = true)
    public Optional<ActiveAcademicContext> resolveForStudent(UUID studentId) {
        UUID yearId =
                firstNonNull(
                        enrollmentMapper.findActiveAcademicYearForStudentFromEnrollments(studentId),
                        classroomRelationMapper.findActiveAcademicYearForStudentFromClassrooms(studentId));
        if (yearId == null) {
            return Optional.empty();
        }
        AcademicYearEntity year = academicYearMapper.findById(yearId);
        if (year == null) {
            return Optional.empty();
        }
        return Optional.of(
                new ActiveAcademicContext(year.getAcademicYearId(), year.getName(), year.getGeneration()));
    }

    private static UUID firstNonNull(UUID a, UUID b) {
        if (a != null) {
            return a;
        }
        return b;
    }
}
