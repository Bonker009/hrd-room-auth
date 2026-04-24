package org.kshrd.hrdroomservice.api.controller;

import static org.kshrd.hrdroomservice.support.Authz.admin;
import static org.kshrd.hrdroomservice.support.Authz.student;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.kshrd.hrdroomservice.api.dto.academic.AcademicYearRequest;
import org.kshrd.hrdroomservice.support.IntegrationTest;
import org.springframework.http.MediaType;

class EnrollmentControllerIT extends IntegrationTest {

    @Test
    void enroll_student_in_basic_course_and_list_by_student() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        UUID basicCourseId = createActiveYearAndReturnBasicCourseId(adminId);

        mockMvc.perform(
                        post("/api/v4/enrollments")
                                .param("studentId", studentId.toString())
                                .param("courseId", basicCourseId.toString())
                                .with(admin(adminId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.data.courseId").value(basicCourseId.toString()))
                .andExpect(jsonPath("$.data.courseType").value("BASIC"))
                .andExpect(jsonPath("$.data.isPassed").value(false))
                .andExpect(jsonPath("$.data.isTerminated").value(false));

        mockMvc.perform(
                        post("/api/v4/enrollments")
                                .param("studentId", studentId.toString())
                                .param("courseId", basicCourseId.toString())
                                .with(admin(adminId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v4/enrollments/student/{id}", studentId).with(student(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseId").value(basicCourseId.toString()));
    }

    @Test
    void student_cannot_read_other_students_enrollments() throws Exception {
        UUID victimId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();

        mockMvc.perform(get("/api/v4/enrollments/student/{id}", victimId).with(student(attackerId)))
                .andExpect(status().isForbidden());
    }

    private UUID createActiveYearAndReturnBasicCourseId(UUID adminId) throws Exception {
        AcademicYearRequest request =
                new AcademicYearRequest(
                        "Gen " + System.nanoTime(),
                        2030,
                        LocalDateTime.of(2030, 1, 1, 0, 0),
                        LocalDateTime.of(2030, 12, 31, 0, 0));

        String created =
                mockMvc.perform(
                                post("/api/v4/academic-years")
                                        .with(admin(adminId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        UUID yearId =
                UUID.fromString(
                        objectMapper
                                .readTree(created)
                                .path("data")
                                .path("academicYearId")
                                .asText());

        mockMvc.perform(put("/api/v4/academic-years/{id}/activate", yearId).with(admin(adminId)))
                .andExpect(status().isOk());

        String list =
                mockMvc.perform(
                                get("/api/v4/courses")
                                        .param("type", "BASIC")
                                        .param("academicYearId", yearId.toString())
                                        .with(admin(adminId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        JsonNode basic = objectMapper.readTree(list).path("data").get(0);
        return UUID.fromString(basic.path("courseId").asText());
    }
}
