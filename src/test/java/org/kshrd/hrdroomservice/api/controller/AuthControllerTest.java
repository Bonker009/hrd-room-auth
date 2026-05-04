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
import org.kshrd.hrdroomservice.api.dto.auth.LoginRequest;
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
    void login_returnsWrappedTokenResponse() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(
                        new AuthTokenResponse(
                                "access-jwt", "refresh", 300L, 1800L, "Bearer", "openid"));

        mockMvc.perform(
                        post("/api/v4/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest("user", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt"));

        verify(authService).login(any(LoginRequest.class));
    }
}
