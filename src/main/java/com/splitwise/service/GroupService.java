package com.splitwise.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitwise.dto.CreateGroupRequest;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional
    public Group createGroup(CreateGroupRequest request) {
        User createdBy = userRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Set<User> members = new HashSet<>();
        members.add(createdBy);

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<User> addedMembers = userRepository.findAllById(request.getMemberIds());
            members.addAll(addedMembers);
        }

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(createdBy)
                .members(members)
                .build();

        return groupRepository.save(group);
    }

    public Optional<Group> getGroupById(Long id) {
        return groupRepository.findById(id);
    }

    @Transactional
    public void addMember(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        group.getMembers().add(user);
        groupRepository.save(group);
    }
}
