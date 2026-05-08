package org.kshrd.hrdroomservice.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kshrd.hrdroomservice.api.dto.classroom.ClassroomResponse;
import org.kshrd.hrdroomservice.api.dto.classroom.CourseSummary;
import org.kshrd.hrdroomservice.api.dto.classroom.StudentCurrentClassroomResponse;
import org.kshrd.hrdroomservice.service.classroom.ClassroomService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ClassroomControllerIT {

    @Mock private ClassroomService classroomService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClassroomController(classroomService)).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void student_can_get_current_classroom_with_generation_and_courses() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        int generation = 12;

        authenticateStudent(studentId);
        when(classroomService.currentForStudent(studentId))
                .thenReturn(
                        new StudentCurrentClassroomResponse(
                                new ClassroomResponse(
                                        classroomId,
                                        "Mobile Development",
                                        "MOB",
                                        "Room for mobile students",
                                        null,
                                        academicYearId,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null),
                                academicYearId,
                                "2026 Academic Year",
                                generation,
                                List.of(
                                        new CourseSummary(
                                                courseId, "BASIC-001", "Basic Course", "BASIC"))));

        mockMvc.perform(get("/api/v4/classrooms/my-classroom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classroom.classroomId").value(classroomId.toString()))
                .andExpect(jsonPath("$.data.academicYearId").value(academicYearId.toString()))
                .andExpect(jsonPath("$.data.academicYearName").value("2026 Academic Year"))
                .andExpect(jsonPath("$.data.generation").value(generation))
                .andExpect(jsonPath("$.data.courses.length()").value(1))
                .andExpect(jsonPath("$.data.courses[0].courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.data.courses[0].code").value("BASIC-001"))
                .andExpect(jsonPath("$.data.courses[0].name").value("Basic Course"))
                .andExpect(jsonPath("$.data.courses[0].type").value("BASIC"));

        verify(classroomService).currentForStudent(studentId);
    }

    private void authenticateStudent(UUID studentId) {
        Jwt jwt =
                new Jwt(
                        "token",
                        null,
                        null,
                        Map.of("alg", "none"),
                        Map.of("sub", studentId.toString()));
        AbstractAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_student")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
