package com.splitwise.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitwise.dto.CreateGroupRequest;
import com.splitwise.dto.GroupResponseDTO;
import com.splitwise.dto.UserSummaryDTO;
import com.splitwise.entity.Expense;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;
import com.splitwise.validator.GroupValidator;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupValidator groupValidator;

    @Transactional
    public GroupResponseDTO createGroup(CreateGroupRequest request) {
        User creator = userRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new EntityNotFoundException("Creator not found"));

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .build();

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<User> members = userRepository.findAllById(request.getMemberIds());
            group.getMembers().addAll(members);
        }
        
        // Add creator as member if not already present
        group.getMembers().add(creator);

        groupValidator.validateAndThrow(group, "group");

        group = groupRepository.save(group);
        return mapToDTO(group);
    }

    @Transactional(readOnly = true)
    public Optional<GroupResponseDTO> getGroupById(Long id) {
        return groupRepository.findById(id).map(this::mapToDTO);
    }

    private GroupResponseDTO mapToDTO(Group group) {
        return GroupResponseDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdAt(group.getCreatedAt())
                .createdBy(toUserSummary(group.getCreatedBy()))
                .members(group.getMembers().stream()
                        .map(this::toUserSummary)
                        .toList())
                .expenses(group.getExpenses().stream()
                        .map(this::toExpenseSummary)
                        .toList())
                .build();
    }

    private UserSummaryDTO toUserSummary(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    private GroupResponseDTO.ExpenseSummaryDTO toExpenseSummary(Expense expense) {
        return GroupResponseDTO.ExpenseSummaryDTO.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
