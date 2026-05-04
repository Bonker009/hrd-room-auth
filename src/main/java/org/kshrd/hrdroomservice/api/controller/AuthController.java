package org.kshrd.hrdroomservice.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.dto.auth.LoginRequest;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.kshrd.hrdroomservice.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v4/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseUtil.ok(authService.login(request), "Login successful");
    }
}
