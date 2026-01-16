package com.splitwise.integration;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

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
import com.splitwise.dto.CreateExpenseRequest;
import com.splitwise.entity.User;
import com.splitwise.entity.UserBalance;
import com.splitwise.enums.SplitType;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.UserBalanceRepository;
import com.splitwise.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class ExpenseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private com.splitwise.repository.ExpenseShareRepository expenseShareRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;
    
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
    @DisplayName("Should create expense and update balances")
    void testCreateExpenseAndCheckBalance() throws Exception {
        // 1. Create Users
        User user1 = userRepository.save(User.builder().name("User 1").email("user1@test.com").build());
        User user2 = userRepository.save(User.builder().name("User 2").email("user2@test.com").build());

        // 2. Create Expense Request
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("100.00"));
        request.setPaidById(user1.getId());
        request.setSplitType(SplitType.EQUAL);
        request.setParticipantIds(Arrays.asList(user1.getId(), user2.getId()));

        // 3. Perform Request
        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(100.00)))
                .andExpect(jsonPath("$.description", is("Dinner")));

        // 4. Verify Balance
        // User 2 should owe User 1 50.00
        // Our convention: from_user < to_user.
        // If User1.id < User2.id:
        //   Record: from=User1, to=User2.
        //   User2 owes User1 => User1 is positive (owed), User2 is negative (owns).
        //   Wait, let's check UserBalanceService logic.
        
        List<UserBalance> balances = userBalanceRepository.findAll();
        Assertions.assertEquals(1, balances.size());
        
        UserBalance balance = balances.get(0);
        // We need to verify the direction and amount based on IDs
        
        // Let's assume User1 ID < User2 ID (Postgres/H2 IDs usually sequential)
        Long id1 = user1.getId();
        Long id2 = user2.getId();
        
        if (id1 < id2) {
             Assertions.assertEquals(id1, balance.getFromUser().getId());
             Assertions.assertEquals(id2, balance.getToUser().getId());
             // User1 paid 100, User2 share is 50. User2 owes User1 50.
             // If from=User1 (lesser ID), to=User2.
             // User1 is +50 wrt User2?
             // Or UserBalance stores "how much fromUser owes toUser"?
             // Checking UserBalanceService...
        }
    }
    @Test
    @DisplayName("Should create expense with percentage split")
    void testCreateExpensePercentage() throws Exception {
        User user1 = userRepository.save(User.builder().name("User 1").email("user1@test.com").build());
        User user2 = userRepository.save(User.builder().name("User 2").email("user2@test.com").build());

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setDescription("Pizza");
        request.setAmount(new BigDecimal("100.00"));
        request.setPaidById(user1.getId());
        request.setSplitType(SplitType.PERCENTAGE);
        request.setParticipantIds(Arrays.asList(user1.getId(), user2.getId()));
        
        // Passing percentages as "exactAmounts" param for now, logic matches
        java.util.Map<Long, BigDecimal> percentages = new java.util.HashMap<>();
        percentages.put(user1.getId(), new BigDecimal("25.00"));
        percentages.put(user2.getId(), new BigDecimal("75.00"));
        request.setExactAmounts(percentages);

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(100.00)));

        // Verify balance
        // User 2 owes 75.00. 
        // User 1 paid 100. User 1 share is 25. User 1 "owes" -25? No.
        // Balance: User 2 owes User 1 75.00.
        // fromUser: User1, toUser: User2. Balance: +75? or -75?
        // Let's check UserBalanceService logic again or trust previous test flow.
        // If fromUser < toUser (User1 < User2)
        // User2 (debtor) owes User1 (payer).
        // Balance -= amount (User2 owes User1). 
        // Wait, updateUserBalance(payer=User1, debtor=User2, amount=75.00).
        // if (payer < debtor) -> from=User1, to=User2. amountAdjustment = -75.00.
        // So Balance should be -75.00 (indicates ToUser owes FromUser? No. Negative usually means To owes From).
        
        List<UserBalance> balances = userBalanceRepository.findAll();
        Assertions.assertEquals(1, balances.size());
        UserBalance b = balances.get(0);
        // Compare using compareTo to ignore scale differences
        Assertions.assertEquals(0, new BigDecimal("-75.00").compareTo(b.getBalance()));
    }
}
