package com.splitwise.service;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.splitwise.dto.CreateUserRequest;
import com.splitwise.entity.User;
import com.splitwise.repository.UserRepository;
import com.splitwise.validator.UserValidator;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserValidator userValidator;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");

        User savedUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(savedUser);

        User result = userService.createUser(request);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getId());
        Assertions.assertEquals("John Doe", result.getName());
        Mockito.verify(userValidator).validateAndThrow(Mockito.any(), Mockito.anyString());
        Mockito.verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUser() {
        User user = User.builder().id(1L).name("John Doe").build();
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUser(1L);

        Assertions.assertEquals("John Doe", result.getName());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testGetUserNotFound() {
        Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> userService.getUser(99L));
    }
}
