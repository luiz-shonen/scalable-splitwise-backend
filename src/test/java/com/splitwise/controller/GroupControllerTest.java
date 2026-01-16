package com.splitwise.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwise.dto.CreateGroupRequest;
import com.splitwise.dto.GroupResponseDTO;
import com.splitwise.dto.UserSummaryDTO;
import com.splitwise.service.GroupService;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupService groupService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateGroup() throws Exception {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Trip");
        request.setCreatedById(1L);

        GroupResponseDTO response = GroupResponseDTO.builder()
                .id(1L)
                .name("Trip")
                .createdBy(UserSummaryDTO.builder().id(1L).name("Alice").build())
                .build();

        Mockito.when(groupService.createGroup(Mockito.any())).thenReturn(response);

        mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Trip"))
                .andExpect(jsonPath("$.createdBy.id").value(1));
    }

    @Test
    void testGetGroup() throws Exception {
        GroupResponseDTO response = GroupResponseDTO.builder()
                .id(1L)
                .name("Trip")
                .build();

        Mockito.when(groupService.getGroupById(1L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Trip"));
    }

    @Test
    void testGetGroup_NotFound() throws Exception {
        Mockito.when(groupService.getGroupById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/groups/99"))
                .andExpect(status().isNotFound());
    }
}
