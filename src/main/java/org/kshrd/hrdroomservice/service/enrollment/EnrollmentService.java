package org.kshrd.hrdroomservice.service.enrollment;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentByCourseTypeResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.TerminateEnrollmentRequest;

public interface EnrollmentService {

    EnrollmentResponse enroll(UUID studentId, UUID courseId, UUID actorId);

    List<EnrollmentResponse> listAll(boolean includeArchived);

    EnrollmentByCourseTypeResponse listByCourseType(boolean includeArchived);

    List<EnrollmentResponse> listByStudent(UUID studentId, boolean includeArchived);

    EnrollmentByCourseTypeResponse listByStudentGrouped(UUID studentId, boolean includeArchived);

    List<EnrollmentResponse> listByCourse(UUID courseId);

    EnrollmentResponse getById(UUID enrollmentId);

    EnrollmentResponse terminate(UUID enrollmentId, TerminateEnrollmentRequest request, UUID actorId);

    EnrollmentResponse reactivate(UUID enrollmentId, UUID actorId);

    EnrollmentResponse moveToAdvanced(UUID studentId, UUID basicCourseId, UUID advancedCourseId, UUID actorId);
}
