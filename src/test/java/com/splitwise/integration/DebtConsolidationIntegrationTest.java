package com.splitwise.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.splitwise.entity.User;
import com.splitwise.enums.SplitType;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.ExpenseShareRepository;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserBalanceRepository;
import com.splitwise.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DebtConsolidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseShareRepository expenseShareRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        expenseShareRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
        userBalanceRepository.deleteAllInBatch();
        groupRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        alice = userRepository.save(User.builder().name("Alice").email("alice@test.com").build());
        bob = userRepository.save(User.builder().name("Bob").email("bob@test.com").build());
        charlie = userRepository.save(User.builder().name("Charlie").email("charlie@test.com").build());
    }

    @Test
    @DisplayName("Complex scenario: Cross-expenses between 3 users should consolidate correctly")
    void testComplexDebtConsolidation() throws Exception {
        // Scenario:
        // 1. Alice pays 90 for Alice, Bob, Charlie (30 each). 
        //    Bob owes Alice 30, Charlie owes Alice 30.
        // 2. Bob pays 60 for Bob, Charlie (30 each).
        //    Charlie owes Bob 30.
        
        // Expense 1: Alice pays 90
        CreateExpenseRequest req1 = new CreateExpenseRequest();
        req1.setDescription("Lunch Alice");
        req1.setAmount(new BigDecimal("90.00"));
        req1.setPaidById(alice.getId());
        req1.setSplitType(SplitType.EQUAL);
        req1.setParticipantIds(Arrays.asList(alice.getId(), bob.getId(), charlie.getId()));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        // Expense 2: Bob pays 60
        CreateExpenseRequest req2 = new CreateExpenseRequest();
        req2.setDescription("Taxi Bob");
        req2.setAmount(new BigDecimal("60.00"));
        req2.setPaidById(bob.getId());
        req2.setSplitType(SplitType.EQUAL);
        req2.setParticipantIds(Arrays.asList(bob.getId(), charlie.getId()));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        // Verification for Alice (ID 1)
        // Alice should see:
        // - Bob owes Alice 30
        // - Charlie owes Alice 30
        mockMvc.perform(get("/api/balances/user/" + alice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owedToUser.length()").value(2))
                .andExpect(jsonPath("$.owedByUser.length()").value(0));

        // Verification for Charlie (ID 3)
        // Charlie should see:
        // - Ower Alice 30
        // - Owes Bob 30
        mockMvc.perform(get("/api/balances/user/" + charlie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owedByUser.length()").value(2))
                .andExpect(jsonPath("$.owedToUser.length()").value(0));

        // Verification for Bob (ID 2)
        // Bob owes Alice 30
        // Charlie owes Bob 30
        mockMvc.perform(get("/api/balances/user/" + bob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owedByUser[0].amount").value(30.00))
                .andExpect(jsonPath("$.owedToUser[0].amount").value(30.00));
    }
}
