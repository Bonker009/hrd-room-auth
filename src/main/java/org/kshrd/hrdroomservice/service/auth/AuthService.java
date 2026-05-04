package org.kshrd.hrdroomservice.service.auth;

import java.util.UUID;
import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.dto.auth.LoginRequest;

public interface AuthService {

    AuthTokenResponse login(LoginRequest request);

    void changeStudentToTeacher(UUID userId);
}
