package com.splitwise.integration;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

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
import com.splitwise.dto.CreateExpenseRequest;
import com.splitwise.dto.SettleRequestDTO;
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
@org.springframework.transaction.annotation.Transactional
public class SettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        expenseShareRepository.deleteAll();
        expenseRepository.deleteAll();
        userBalanceRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(User.builder().name("Alice").email("alice@test.com").build());
        bob = userRepository.save(User.builder().name("Bob").email("bob@test.com").build());
    }

    @Test
    @DisplayName("Should successfully settle a debt between two users")
    void testSettlePayment() throws Exception {
        // 1. Create an expense: Alice paid 100, shared equally with Bob
        CreateExpenseRequest expenseRequest = new CreateExpenseRequest();
        expenseRequest.setPaidById(alice.getId());
        expenseRequest.setDescription("Dinner");
        expenseRequest.setAmount(new BigDecimal("100.00"));
        expenseRequest.setSplitType(SplitType.EQUAL);
        expenseRequest.setParticipantIds(List.of(alice.getId(), bob.getId()));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isOk());

        // Verify balance: Bob owes Alice 50
        mockMvc.perform(get("/api/balances/user/" + bob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owedByUser", hasSize(1)))
                .andExpect(jsonPath("$.owedByUser[0].user.name").value("Alice"))
                .andExpect(jsonPath("$.owedByUser[0].amount", comparesEqualTo(50.0)));

        // 2. Bob settles 30 with Alice
        SettleRequestDTO settleRequest = SettleRequestDTO.builder()
                .fromUserId(bob.getId())
                .toUserId(alice.getId())
                .amount(new BigDecimal("30.00"))
                .build();

        mockMvc.perform(post("/api/balances/settle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settleRequest)))
                .andExpect(status().isOk());

        // Verify balance: Bob now owes Alice 20
        mockMvc.perform(get("/api/balances/user/" + bob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owedByUser[0].amount", comparesEqualTo(20.0)));

        // 3. Bob settles remaining 20
        settleRequest.setAmount(new BigDecimal("20.00"));
        mockMvc.perform(post("/api/balances/settle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settleRequest)))
                .andExpect(status().isOk());

        // Verify balance: Bob owes nothing
        mockMvc.perform(get("/api/balances/user/" + bob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owedByUser", hasSize(0)))
                .andExpect(jsonPath("$.owedToUser", hasSize(0)));
    }
}
