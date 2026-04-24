package org.kshrd.hrdroomservice.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.kshrd.hrdroomservice.api.validation.StrongPassword;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @StrongPassword String password,
        String firstName,
        String lastName) {}
