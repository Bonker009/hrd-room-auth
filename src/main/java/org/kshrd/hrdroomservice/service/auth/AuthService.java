package org.kshrd.hrdroomservice.service.auth;

import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.dto.auth.LoginRequest;
import org.kshrd.hrdroomservice.api.dto.auth.LogoutRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisterRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisteredUserResponse;

public interface AuthService {

    AuthTokenResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    RegisteredUserResponse register(RegisterRequest request);
}
