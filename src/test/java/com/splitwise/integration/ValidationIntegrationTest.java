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
import com.splitwise.dto.ExpenseSplitDTO;
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

    @Test
    @DisplayName("Should return 400 when splitType is EQUAL but splitDetails are provided")
    void testEqualSplitConflictValidation() throws Exception {
        User user1 = userRepository.save(User.builder().name("User 1").email("user1@test.com").build());
        User user2 = userRepository.save(User.builder().name("User 2").email("user2@test.com").build());

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Confusing Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setPaidById(user1.getId());
        request.setSplitType(SplitType.EQUAL);
        request.setParticipantIds(Arrays.asList(user1.getId(), user2.getId()));
        request.setSplitDetails(Arrays.asList(
                ExpenseSplitDTO.builder().userId(user1.getId()).amount(new BigDecimal("50.00")).build()
        ));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Split details should not be provided for EQUAL")));
    }

    @Test
    @DisplayName("Should return 400 when EXACT split sum does not match total amount")
    void testExactSplitSumMismatch() throws Exception {
        User user1 = userRepository.save(User.builder().name("User 1").email("user1@test.com").build());
        User user2 = userRepository.save(User.builder().name("User 2").email("user2@test.com").build());

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Bad Math Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setPaidById(user1.getId());
        request.setSplitType(SplitType.EXACT);
        request.setParticipantIds(Arrays.asList(user1.getId(), user2.getId()));
        request.setSplitDetails(Arrays.asList(
                ExpenseSplitDTO.builder().userId(user1.getId()).amount(new BigDecimal("40.00")).build(),
                ExpenseSplitDTO.builder().userId(user2.getId()).amount(new BigDecimal("50.00")).build()
        ));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("The sum of split details ([90.00]) must equal the total expense amount ([100.00])")));
    }

    @Test
    @DisplayName("Should return 400 when splitDetail contains user not in participantIds")
    void testSplitParticipantInconsistency() throws Exception {
        User user1 = userRepository.save(User.builder().name("User 1").email("user1@test.com").build());
        User user2 = userRepository.save(User.builder().name("User 2").email("user2@test.com").build());
        User user3 = userRepository.save(User.builder().name("User 3").email("user3@test.com").build());

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Inconsistent Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setPaidById(user1.getId());
        request.setSplitType(SplitType.EXACT);
        request.setParticipantIds(Arrays.asList(user1.getId(), user2.getId()));
        request.setSplitDetails(Arrays.asList(
                ExpenseSplitDTO.builder().userId(user1.getId()).amount(new BigDecimal("50.00")).build(),
                ExpenseSplitDTO.builder().userId(user3.getId()).amount(new BigDecimal("50.00")).build()
        ));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("in split details is not in the participants list")));
    }
}
