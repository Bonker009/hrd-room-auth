package org.kshrd.hrdroomservice.api.controller;

import static org.kshrd.hrdroomservice.support.Authz.admin;
import static org.kshrd.hrdroomservice.support.Authz.student;
import static org.kshrd.hrdroomservice.support.Authz.teacher;
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
import org.kshrd.hrdroomservice.api.dto.course.CourseUpdateRequest;
import org.kshrd.hrdroomservice.support.IntegrationTest;
import org.springframework.http.MediaType;

class CourseControllerIT extends IntegrationTest {

    @Test
    void activating_a_year_seeds_basic_and_advanced_courses_and_they_are_listable()
            throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        UUID yearId = createAndActivateYear(adminId);

        mockMvc.perform(
                        get("/api/v4/courses")
                                .param("academicYearId", yearId.toString())
                                .with(admin(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[?(@.type=='BASIC')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.type=='ADVANCED')]").exists());

        mockMvc.perform(get("/api/v4/courses").param("type", "BASIC").with(teacher(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("BASIC"));

        mockMvc.perform(get("/api/v4/courses").with(student(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void update_changes_course_fields_and_bumps_version() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID yearId = createAndActivateYear(adminId);

        String list =
                mockMvc.perform(
                                get("/api/v4/courses")
                                        .param("type", "BASIC")
                                        .param("academicYearId", yearId.toString())
                                        .with(admin(adminId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        JsonNode basic = objectMapper.readTree(list).path("data").path("content").get(0);
        UUID courseId = UUID.fromString(basic.path("courseId").asText());
        long originalVersion = basic.path("version").asLong();

        CourseUpdateRequest update =
                new CourseUpdateRequest("Revised BASIC", "Revised description");

        mockMvc.perform(
                        put("/api/v4/courses/{id}", courseId)
                                .with(admin(adminId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Revised BASIC"))
                .andExpect(jsonPath("$.data.description").value("Revised description"))
                .andExpect(jsonPath("$.data.version").value((int) (originalVersion + 1)));
    }

    private UUID createAndActivateYear(UUID adminId) throws Exception {
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

        return yearId;
    }
}
