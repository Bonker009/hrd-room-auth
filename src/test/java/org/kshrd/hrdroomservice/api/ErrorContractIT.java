package org.kshrd.hrdroomservice.api;

import static org.kshrd.hrdroomservice.support.Authz.admin;
import static org.kshrd.hrdroomservice.support.Authz.student;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.security.enabled=true")
class ErrorContractIT extends IntegrationTest {

    @Test
    void unauthorized_is_api_response_shape() throws Exception {
        mockMvc.perform(get("/api/v4/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void forbidden_is_api_response_shape() throws Exception {
        mockMvc.perform(
                        get("/api/v4/enrollments/student/{id}", UUID.randomUUID())
                                .with(student(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void not_found_is_api_response_shape() throws Exception {
        mockMvc.perform(
                        get("/api/v4/academic-years/{id}", UUID.randomUUID())
                                .with(admin(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void conflict_is_api_response_shape() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID basicCourseId = createActiveYearAndReturnBasicCourseId(adminId);

        mockMvc.perform(
                        post("/api/v4/enrollments")
                                .param("studentId", studentId.toString())
                                .param("courseId", basicCourseId.toString())
                                .with(admin(adminId)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v4/enrollments")
                                .param("studentId", studentId.toString())
                                .param("courseId", basicCourseId.toString())
                                .with(admin(adminId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void validation_error_contains_violations_details() throws Exception {
        AcademicYearRequest invalid =
                new AcademicYearRequest(
                        " ",
                        999,
                        LocalDateTime.of(2030, 1, 1, 0, 0),
                        LocalDateTime.of(2030, 12, 31, 0, 0));

        mockMvc.perform(
                        post("/api/v4/academic-years")
                                .with(admin(UUID.randomUUID()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.violations[0].field").value("name"));
    }

    @Test
    void malformed_json_returns_bad_request_shape() throws Exception {
        String malformedDatePayload =
                """
                {
                  "name": "Gen 2031",
                  "generation": 2031,
                  "startDate": "2025-07-09",
                  "endDate": "2031-12-31T00:00:00"
                }
                """;

        mockMvc.perform(
                        post("/api/v4/academic-years")
                                .with(admin(UUID.randomUUID()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedDatePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.details.field").value("startDate"));
    }

    @Test
    void path_type_mismatch_returns_bad_request_shape() throws Exception {
        mockMvc.perform(
                        get("/api/v4/academic-years/{id}", "not-a-uuid")
                                .with(admin(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.details.field").value("id"));
    }

    @Test
    void method_not_allowed_returns_proper_shape() throws Exception {
        mockMvc.perform(patch("/api/v4/academic-years").with(admin(UUID.randomUUID())))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(405))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupported_media_type_returns_proper_shape() throws Exception {
        mockMvc.perform(
                        post("/api/v4/academic-years")
                                .with(admin(UUID.randomUUID()))
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("plain-text-body"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(415))
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
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
