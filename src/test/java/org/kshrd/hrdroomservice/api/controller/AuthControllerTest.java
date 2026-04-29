package org.kshrd.hrdroomservice.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.dto.auth.RefreshTokenRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisterRequest;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.api.exception.GlobalExceptionHandler;
import org.kshrd.hrdroomservice.service.auth.AuthService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AuthController(authService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setValidator(validator)
                        .build();
    }

    @Test
    void refresh_returnsWrappedTokenResponse() throws Exception {
        when(authService.refresh(any(RefreshTokenRequest.class)))
                .thenReturn(
                        new AuthTokenResponse(
                                "access-jwt", "new-refresh", 300L, 1800L, "Bearer", "openid"));

        mockMvc.perform(
                        post("/api/v4/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RefreshTokenRequest("old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));

        verify(authService).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    void register_dummyEmail_returnsRegistrationRejected() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(
                        ApiException.registrationRejected(
                                "Disposable email addresses are not allowed", "email"));

        mockMvc.perform(
                        post("/api/v4/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RegisterRequest(
                                                        "student01",
                                                        "foo@mailinator.com",
                                                        "Abcd1234@",
                                                        "John",
                                                        "Doe"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REGISTRATION_REJECTED"))
                .andExpect(jsonPath("$.details.field").value("email"));
    }
}
