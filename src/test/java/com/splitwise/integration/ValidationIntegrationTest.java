package com.splitwise.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwise.dto.CreateExpenseRequest;
import com.splitwise.dto.CreateGroupRequest;
import com.splitwise.dto.CreateUserRequest;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.enums.SplitType;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return 400 when creating user with duplicate email")
    void testDuplicateEmailValidation() throws Exception {
        userRepository.save(User.builder().name("Original").email("duplicate@test.com").build());

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Impostor");
        request.setEmail("duplicate@test.com");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Email is already in use")));
    }

    @Test
    @DisplayName("Should return 400 when creating group with empty name")
    void testEmptyGroupNameValidation() throws Exception {
        User creator = userRepository.save(User.builder().name("Creator").email("creator@test.com").build());

        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("");
        request.setCreatedById(creator.getId());

        mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Input validation failed")));
    }

    @Test
    @DisplayName("Should return 400 when payer is not in group")
    void testPayerNotInGroupValidation() throws Exception {
        User groupMember = userRepository.save(User.builder().name("Member").email("member@test.com").build());
        User outsider = userRepository.save(User.builder().name("Outsider").email("outsider@test.com").build());
        
        Group group = Group.builder().name("Trips").createdBy(groupMember).build();
        group.getMembers().add(groupMember);
        group = groupRepository.save(group);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Illegal Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setPaidById(outsider.getId());
        request.setGroupId(group.getId());
        request.setSplitType(SplitType.EQUAL);
        request.setParticipantIds(Arrays.asList(groupMember.getId()));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to group")));
    }
}
