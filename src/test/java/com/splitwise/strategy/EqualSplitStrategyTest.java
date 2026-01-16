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
import java.util.List;

class EqualSplitStrategyTest {

    private EqualSplitStrategy strategy;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        strategy = new EqualSplitStrategy();
        user1 = User.builder().id(1L).name("User 1").build();
        user2 = User.builder().id(2L).name("User 2").build();
        user3 = User.builder().id(3L).name("User 3").build();
    }

    @Test
    @DisplayName("Should split amount exactly equally when possible")
    void testExactSplit() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .build();
        
        List<User> participants = Arrays.asList(user1, user2);

        List<ExpenseShare> shares = strategy.split(expense, participants, Collections.emptyMap());

        Assertions.assertEquals(2, shares.size());
        Assertions.assertEquals(new BigDecimal("50.00"), shares.get(0).getAmount());
        Assertions.assertEquals(new BigDecimal("50.00"), shares.get(1).getAmount());
    }

    @Test
    @DisplayName("Should distribute remainder cents to first participants")
    void testRemainderSplit() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .build();
        
        // 100 / 3 = 33.333... -> 33.34, 33.33, 33.33
        List<User> participants = Arrays.asList(user1, user2, user3);

        List<ExpenseShare> shares = strategy.split(expense, participants, Collections.emptyMap());

        Assertions.assertEquals(3, shares.size());
        
        // First participant gets the extra cent
        Assertions.assertEquals(new BigDecimal("33.34"), shares.get(0).getAmount());
        Assertions.assertEquals(user1, shares.get(0).getUser());
        
        Assertions.assertEquals(new BigDecimal("33.33"), shares.get(1).getAmount());
        Assertions.assertEquals(user2, shares.get(1).getUser());
        
        Assertions.assertEquals(new BigDecimal("33.33"), shares.get(2).getAmount());
        Assertions.assertEquals(user3, shares.get(2).getUser());
        
        // Validation sum
        BigDecimal sum = shares.stream()
                .map(ExpenseShare::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertEquals(new BigDecimal("100.00"), sum);
    }
    
    @Test
    @DisplayName("Should handle complex remainder distribution")
    void testComplexRemainderSplit() {
        // 10.00 / 6 = 1.666...
        // Base = 1.66
        // Total Base = 1.66 * 6 = 9.96
        // Remainder = 0.04
        // Shares: 1.67, 1.67, 1.67, 1.67, 1.66, 1.66
        Expense expense = Expense.builder()
                .amount(new BigDecimal("10.00"))
                .build();
        
        User user4 = User.builder().id(4L).build();
        User user5 = User.builder().id(5L).build();
        User user6 = User.builder().id(6L).build();
        
        List<User> participants = Arrays.asList(user1, user2, user3, user4, user5, user6);

        List<ExpenseShare> shares = strategy.split(expense, participants, Collections.emptyMap());
        
        Assertions.assertEquals(new BigDecimal("1.67"), shares.get(0).getAmount());
        Assertions.assertEquals(new BigDecimal("1.67"), shares.get(1).getAmount());
        Assertions.assertEquals(new BigDecimal("1.67"), shares.get(2).getAmount());
        Assertions.assertEquals(new BigDecimal("1.67"), shares.get(3).getAmount());
        Assertions.assertEquals(new BigDecimal("1.66"), shares.get(4).getAmount());
        Assertions.assertEquals(new BigDecimal("1.66"), shares.get(5).getAmount());
    }

    @Test
    @DisplayName("Should throw exception if validation fails")
    void testValidation() {
        Expense expense = Expense.builder().amount(new BigDecimal("100.00")).build();
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            strategy.split(expense, Collections.emptyList(), Collections.emptyMap())
        );
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            strategy.split(null, Arrays.asList(user1), Collections.emptyMap())
        );
    }
}
