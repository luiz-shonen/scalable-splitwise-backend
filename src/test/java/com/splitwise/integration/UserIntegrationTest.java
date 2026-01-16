package com.splitwise.integration;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwise.dto.CreateUserRequest;
import com.splitwise.entity.User;
import com.splitwise.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private com.splitwise.repository.ExpenseShareRepository expenseShareRepository;
    @Autowired
    private com.splitwise.repository.ExpenseRepository expenseRepository;
    @Autowired
    private com.splitwise.repository.UserBalanceRepository userBalanceRepository;
    @Autowired
    private com.splitwise.repository.GroupRepository groupRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        expenseShareRepository.deleteAll();
        expenseRepository.deleteAll();
        userBalanceRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create user via API")
    void testCreateUserApi() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Integration User");
        request.setEmail("integration@test.com");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Integration User")))
                .andExpect(jsonPath("$.email", is("integration@test.com")));
        
        Assertions.assertEquals(1, userRepository.count());
    }

    @Test
    @DisplayName("Should return bad request when validation fails")
    void testCreateUserValidation() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(""); // Invalid
        request.setEmail("invalid-email"); // Invalid

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get user via API")
    void testGetUserApi() throws Exception {
        User user = User.builder()
                .name("Existing User")
                .email("existing@test.com")
                .build();
        userRepository.save(user);

        mockMvc.perform(get("/api/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Existing User")));
    }
}
