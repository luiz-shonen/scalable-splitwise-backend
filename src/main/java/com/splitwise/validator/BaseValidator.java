package com.splitwise.validator;

import java.util.stream.Collectors;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;

import com.splitwise.exception.ValidationException;

public interface BaseValidator extends SmartValidator {

    default void validateAndThrow(Object target, String objectName) {
        Errors errors = new BeanPropertyBindingResult(target, objectName);
        this.validate(target, errors);

        if (errors.hasErrors()) {
            String errorMessage = errors.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            throw new ValidationException(errorMessage);
        }
    }
}
