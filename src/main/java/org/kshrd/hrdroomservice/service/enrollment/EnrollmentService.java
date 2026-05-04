package org.kshrd.hrdroomservice.service.enrollment;

import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentByCourseTypeResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.EnrollmentResponse;
import org.kshrd.hrdroomservice.api.dto.enrollment.TerminateEnrollmentRequest;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;

public interface EnrollmentService {

    EnrollmentResponse enroll(UUID studentId, UUID courseId, UUID actorId);

    PageResponse<EnrollmentResponse> listAll(boolean includeArchived, int page, int size);

    EnrollmentByCourseTypeResponse listByCourseType(boolean includeArchived);

    PageResponse<EnrollmentResponse> listByStudent(
            UUID studentId, boolean includeArchived, int page, int size);

    EnrollmentByCourseTypeResponse listByStudentGrouped(UUID studentId, boolean includeArchived);

    PageResponse<EnrollmentResponse> listByCourse(UUID courseId, int page, int size);

    EnrollmentResponse getById(UUID enrollmentId);

    EnrollmentResponse terminate(
            UUID enrollmentId, TerminateEnrollmentRequest request, UUID actorId);

    EnrollmentResponse reactivate(UUID enrollmentId, UUID actorId);

    EnrollmentResponse moveToAdvanced(
            UUID studentId, UUID basicCourseId, UUID advancedCourseId, UUID actorId);
}
