package org.kshrd.hrdroomservice.api.controller;

import static org.kshrd.hrdroomservice.support.Authz.admin;
import static org.kshrd.hrdroomservice.support.Authz.student;
import static org.kshrd.hrdroomservice.support.Authz.teacher;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.kshrd.hrdroomservice.service.auth.AuthService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AccountControllerIT extends org.kshrd.hrdroomservice.support.IntegrationTest {

    @MockitoBean private AuthService authService;

    @Test
    void admin_can_change_student_to_teacher_role() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        mockMvc.perform(
                        put("/api/v4/account/{userId}/role/teacher", targetUserId)
                                .with(admin(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role changed from student to teacher"));

        verify(authService).changeStudentToTeacher(targetUserId);
    }

    @Test
    void non_admin_users_cannot_change_role() throws Exception {
        UUID targetUserId = UUID.randomUUID();

        mockMvc.perform(
                        put("/api/v4/account/{userId}/role/teacher", targetUserId)
                                .with(student(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/api/v4/account/{userId}/role/teacher", targetUserId)
                                .with(teacher(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        verify(authService, never()).changeStudentToTeacher(targetUserId);
    }
}
