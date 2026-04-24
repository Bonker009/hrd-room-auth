package org.kshrd.hrdroomservice.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kshrd.hrdroomservice.api.dto.auth.RegisterRequest;

class StrongPasswordValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void strongPassword_passes() {
        RegisterRequest request =
                new RegisterRequest("user1", "user1@example.com", "Aa1!bcde", "A", "B");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void weakPassword_missingComplexity_fails() {
        RegisterRequest request =
                new RegisterRequest("user1", "user1@example.com", "password", "A", "B");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")))
                .isTrue();
    }

    @Test
    void weakPassword_tooShort_fails() {
        RegisterRequest request =
                new RegisterRequest("user1", "user1@example.com", "Aa1!x", "A", "B");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
}
