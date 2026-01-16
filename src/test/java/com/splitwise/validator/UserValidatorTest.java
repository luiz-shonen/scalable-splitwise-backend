package com.splitwise.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.splitwise.entity.User;
import com.splitwise.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserValidator userValidator;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("Alice")
                .email("alice@test.com")
                .build();
    }

    @Test
    void testValidatePositive() {
        Mockito.when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);

        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);

        Assertions.assertFalse(errors.hasErrors());
    }

    @Test
    void testValidateDuplicateEmail() {
        Mockito.when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        Errors errors = new BeanPropertyBindingResult(user, "user");
        userValidator.validate(user, errors);

        Assertions.assertTrue(errors.hasErrors());
        Assertions.assertEquals("Email is already in use: alice@test.com", 
            errors.getFieldError("email").getDefaultMessage());
    }
}
