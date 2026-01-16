package com.splitwise.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.splitwise.dto.CreateGroupRequest;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    @Test
    @DisplayName("Should create group successfully")
    void testCreateGroup() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Trip");
        request.setDescription("Europe Trip");
        request.setCreatedById(1L);

        User creator = User.builder().id(1L).name("John").build();
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        Group savedGroup = Group.builder()
                .id(1L)
                .name("Trip")
                .description("Europe Trip")
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Simulating the save behavior
        Mockito.when(groupRepository.save(Mockito.any(Group.class))).thenReturn(savedGroup);

        Group result = groupService.createGroup(request);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Trip", result.getName());
        Assertions.assertEquals(creator, result.getCreatedBy());
        Mockito.verify(groupRepository).save(Mockito.any(Group.class));
    }

    @Test
    @DisplayName("Should add member to group successfully")
    void testAddMember() {
        Group group = Group.builder().id(1L).name("Trip").members(new java.util.HashSet<>()).build();
        User user = User.builder().id(2L).name("Jane").build();

        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        groupService.addMember(1L, 2L);

        Mockito.verify(groupRepository).save(group);
        Assertions.assertTrue(group.getMembers().contains(user));
    }

    @Test
    @DisplayName("Should throw exception when group not found")
    void testAddMemberGroupNotFound() {
        Mockito.when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> groupService.addMember(99L, 2L));
    }
}
