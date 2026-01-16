package com.splitwise.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import com.splitwise.entity.User;
import com.splitwise.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Validator for User domain rules.
 */
@Component
@RequiredArgsConstructor
public class UserValidator implements BaseValidator {

    private final UserRepository userRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        validate(target, errors, (Object[]) null);
    }

    @Override
    public void validate(Object target, Errors errors, Object... validationHints) {
        User user = (User) target;

        // Domain rule: Email must be unique
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            errors.rejectValue("email", "user.email.duplicate", "Email is already in use: " + user.getEmail());
        }
    }
}
