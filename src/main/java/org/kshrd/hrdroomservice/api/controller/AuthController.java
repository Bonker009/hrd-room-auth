package org.kshrd.hrdroomservice.api.controller;

import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.dto.auth.LoginRequest;
import org.kshrd.hrdroomservice.api.dto.auth.LogoutRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RefreshTokenRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisterRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisteredUserResponse;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.kshrd.hrdroomservice.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v4/auth")
@RequiredArgsConstructor

public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseUtil.ok(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseUtil.ok(authService.refresh(request), "Token refreshed");
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseUtil.ok(null, "Logout successful");
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisteredUserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseUtil.created(authService.register(request), "Registration successful");
    }
}
