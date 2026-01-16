package com.splitwise.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitwise.dto.CreateUserRequest;
import com.splitwise.dto.UserResponseDTO;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.repository.UserRepository;
import com.splitwise.validator.UserValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;

    @Transactional
    public UserResponseDTO createUser(CreateUserRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
        
        userValidator.validateAndThrow(user, "user");

        return mapToDTO(userRepository.save(user));
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public Optional<UserResponseDTO> getUserResponseById(Long id) {
        return userRepository.findById(id).map(this::mapToDTO);
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found: " + id));
    }

    public List<User> getUsersByIds(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    private UserResponseDTO mapToDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .groupIds(user.getGroups().stream().map(Group::getId).toList())
                .build();
    }
}
