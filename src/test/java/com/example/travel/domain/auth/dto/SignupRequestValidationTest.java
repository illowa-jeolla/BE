package com.example.travel.domain.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignupRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsNormalizedTenCharacterNickname() {
        assertThat(validator.validate(new SignupRequest(
                "user@example.com", "password123", "  1234567890  "))).isEmpty();
    }

    @Test
    void rejectsNormalizedNicknameLongerThanTenCharacters() {
        assertThat(validator.validate(new SignupRequest(
                "user@example.com", "password123", "  12345678901  ")))
                .anySatisfy(violation ->
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("nickname"));
    }
}
