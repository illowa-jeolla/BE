package com.example.travel.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TrimmedSizeValidator implements ConstraintValidator<TrimmedSize, String> {
    private int max;

    @Override
    public void initialize(TrimmedSize annotation) {
        max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.trim().length() <= max;
    }
}
