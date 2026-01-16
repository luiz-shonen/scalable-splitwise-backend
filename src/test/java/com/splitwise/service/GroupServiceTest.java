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
import com.splitwise.dto.GroupResponseDTO;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;

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

        User creator = User.builder().id(1L).name("John").email("john@test.com").build();
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        Group savedGroup = Group.builder()
                .id(1L)
                .name("Trip")
                .description("Europe Trip")
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .members(new java.util.HashSet<>())
                .expenses(new java.util.ArrayList<>())
                .build();
        
        savedGroup.getMembers().add(creator);
        
        Mockito.when(groupRepository.save(Mockito.any(Group.class))).thenReturn(savedGroup);

        GroupResponseDTO result = groupService.createGroup(request);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Trip", result.getName());
        Assertions.assertEquals(1L, result.getCreatedBy().getId());
        Mockito.verify(groupRepository).save(Mockito.any(Group.class));
    }

    @Test
    @DisplayName("Should get group by id successfully")
    void testGetGroupById() {
        User creator = User.builder().id(1L).name("John").build();
        Group group = Group.builder()
                .id(1L)
                .name("Trip")
                .createdBy(creator)
                .members(new java.util.HashSet<>())
                .expenses(new java.util.ArrayList<>())
                .build();

        Mockito.when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        Optional<GroupResponseDTO> result = groupService.getGroupById(1L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Trip", result.get().getName());
    }
}
