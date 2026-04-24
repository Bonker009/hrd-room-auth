package org.kshrd.hrdroomservice.api.controller;

import static org.kshrd.hrdroomservice.support.Authz.admin;
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
import org.springframework.test.web.servlet.MvcResult;

class AcademicYearControllerIT extends IntegrationTest {

    @Test
    void create_activate_and_fetch_flow() throws Exception {
        UUID adminId = UUID.randomUUID();

        AcademicYearRequest request =
                new AcademicYearRequest(
                        "Generation " + System.currentTimeMillis(),
                        42,
                        LocalDateTime.of(2030, 1, 1, 0, 0),
                        LocalDateTime.of(2030, 12, 31, 0, 0));

        MvcResult createResult =
                mockMvc.perform(
                                post("/api/v4/academic-years")
                                        .with(admin(adminId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.name").value(request.name()))
                        .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                        .andExpect(jsonPath("$.data.academicYearId").exists())
                        .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID yearId = UUID.fromString(body.path("data").path("academicYearId").asText());

        mockMvc.perform(put("/api/v4/academic-years/{id}/activate", yearId).with(admin(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v4/academic-years/{id}", yearId).with(admin(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.academicYearId").value(yearId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.generation").value(42));

        mockMvc.perform(get("/api/v4/academic-years/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.academicYearId").value(yearId.toString()));
    }
}
