package com.splitwise.strategy;

import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ExactAmountStrategyTest {

    private ExactAmountStrategy strategy;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        strategy = new ExactAmountStrategy();
        user1 = User.builder().id(1L).name("User 1").build();
        user2 = User.builder().id(2L).name("User 2").build();
    }

    @Test
    @DisplayName("Should create shares when exact amounts match total")
    void testValidSplit() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .build();
        
        List<User> participants = Arrays.asList(user1, user2);
        
        Map<Long, BigDecimal> exactAmounts = new HashMap<>();
        exactAmounts.put(1L, new BigDecimal("60.00"));
        exactAmounts.put(2L, new BigDecimal("40.00"));

        List<ExpenseShare> shares = strategy.split(expense, participants, exactAmounts);

        Assertions.assertEquals(2, shares.size());
        
        ExpenseShare share1 = shares.stream().filter(s -> s.getUser().equals(user1)).findFirst().orElseThrow();
        Assertions.assertEquals(new BigDecimal("60.00"), share1.getAmount());

        ExpenseShare share2 = shares.stream().filter(s -> s.getUser().equals(user2)).findFirst().orElseThrow();
        Assertions.assertEquals(new BigDecimal("40.00"), share2.getAmount());
    }

    @Test
    @DisplayName("Should throw exception if sum does not match total")
    void testSumMismatch() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .build();
        
        List<User> participants = Arrays.asList(user1, user2);
        
        Map<Long, BigDecimal> exactAmounts = new HashMap<>();
        exactAmounts.put(1L, new BigDecimal("50.00"));
        exactAmounts.put(2L, new BigDecimal("40.00")); // Sum = 90 != 100

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> 
            strategy.split(expense, participants, exactAmounts)
        );
        
        Assertions.assertTrue(exception.getMessage().contains("does not equal expense total"));
    }
    
    @Test
    @DisplayName("Should throw exception if participant is missing amount")
    void testMissingParticipantAmount() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .build();
        
        List<User> participants = Arrays.asList(user1, user2);
        
        Map<Long, BigDecimal> exactAmounts = new HashMap<>();
        exactAmounts.put(1L, new BigDecimal("100.00"));
        // User 2 missing

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> 
            strategy.split(expense, participants, exactAmounts)
        );
        
        Assertions.assertTrue(exception.getMessage().contains("Missing exact amount"));
    }
    
    @Test
    @DisplayName("Should throw exception if amount is negative")
    void testNegativeAmount() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .build();
        
        List<User> participants = Arrays.asList(user1, user2);
        
        Map<Long, BigDecimal> exactAmounts = new HashMap<>();
        exactAmounts.put(1L, new BigDecimal("110.00"));
        exactAmounts.put(2L, new BigDecimal("-10.00"));

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> 
            strategy.split(expense, participants, exactAmounts)
        );
        
        Assertions.assertTrue(exception.getMessage().contains("Invalid amount"));
    }
}
