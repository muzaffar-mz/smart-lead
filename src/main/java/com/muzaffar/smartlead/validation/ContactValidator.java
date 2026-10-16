package com.muzaffar.smartlead.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContactValidator implements ConstraintValidator<ValidContact, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null/blank is reported by @NotBlank
        return value == null || value.isBlank() || ContactUtils.isEmail(value) || ContactUtils.isPhone(value);
    }
}
