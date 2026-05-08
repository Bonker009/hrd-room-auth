package org.kshrd.hrdroomservice.service.classroom;

import java.util.List;
import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.classroom.AssessmentIdsRequest;
import org.kshrd.hrdroomservice.api.dto.classroom.AssessmentSummaryResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomRequest;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomStudentCountResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.StudentCurrentClassroomResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.UuidListRequest;
import org.kshrd.hrdroomservice.api.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ClassroomService {

    ClassroomResponse create(ClassroomRequest request, UUID actorId);

    ClassroomResponse update(UUID classroomId, ClassroomRequest request, UUID actorId);

    void delete(UUID classroomId);

    String uploadImage(UUID classroomId, MultipartFile file, UUID actorId);

    ClassroomResponse getById(UUID classroomId);

    List<AssessmentSummaryResponse> listAssessments(UUID classroomId);

    PageResponse<ClassroomResponse> myClassrooms(UUID teacherId, int page, int size);

    PageResponse<ClassroomResponse> listAll(int page, int size);

    void assignAssessments(UUID classroomId, AssessmentIdsRequest request);

    void removeAssessments(UUID classroomId, AssessmentIdsRequest request);

    List<ClassroomResponse> bySubject(UUID subjectId);

    void assignTeachers(UUID classroomId, UuidListRequest request);

    void removeTeachers(UUID classroomId, UuidListRequest request);

    List<UUID> listTeacherIds(UUID classroomId);

    List<ClassroomResponse> listWithTeachers();

    void assignSubject(UUID classroomId, UUID subjectId);

    void removeSubject(UUID classroomId, UUID subjectId);

    List<UUID> listSubjectIds(UUID classroomId);

    List<ClassroomResponse> listWithSubjects();

    void assignStudents(UUID classroomId, UuidListRequest request);

    void removeStudents(UUID classroomId, UuidListRequest request);

    List<UUID> listStudentIds(UUID classroomId);

    List<ClassroomResponse> listWithStudents();

    List<ClassroomStudentCountResponse> studentCounts();

    void moveStudent(UUID sourceClassroomId, UUID targetClassroomId, UUID studentId);

    StudentCurrentClassroomResponse currentForStudent(UUID studentId);
}
