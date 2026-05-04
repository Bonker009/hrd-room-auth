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

class StrongPasswordValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    /** Local fixture so password policy tests do not depend on removed auth DTOs. */
    private record PasswordSample(@StrongPassword String password) {}

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
        PasswordSample request = new PasswordSample("Aa1!bcde");
        Set<ConstraintViolation<PasswordSample>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void weakPassword_missingComplexity_fails() {
        PasswordSample request = new PasswordSample("password");
        Set<ConstraintViolation<PasswordSample>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(
                        violations.stream()
                                .anyMatch(v -> v.getPropertyPath().toString().equals("password")))
                .isTrue();
    }

    @Test
    void weakPassword_tooShort_fails() {
        PasswordSample request = new PasswordSample("Aa1!x");
        Set<ConstraintViolation<PasswordSample>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
}
