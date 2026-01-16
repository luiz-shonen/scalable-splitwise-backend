package com.splitwise.strategy;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.User;

class PercentageSplitStrategyTest {

    private PercentageSplitStrategy strategy;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        strategy = new PercentageSplitStrategy();
        user1 = User.builder().id(1L).name("User 1").build();
        user2 = User.builder().id(2L).name("User 2").build();
        user3 = User.builder().id(3L).name("User 3").build();
    }

    @Test
    @DisplayName("Should split amount based on percentages")
    void testValidPercentageSplit() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.00"))
                .build();
        
        List<User> participants = Arrays.asList(user1, user2);
        
        Map<Long, BigDecimal> percentages = new HashMap<>();
        percentages.put(1L, new BigDecimal("60.00")); // 60%
        percentages.put(2L, new BigDecimal("40.00")); // 40%

        List<ExpenseShare> shares = strategy.split(expense, participants, percentages);

        Assertions.assertEquals(2, shares.size());
        
        ExpenseShare share1 = shares.stream().filter(s -> s.getUser().equals(user1)).findFirst().orElseThrow();
        Assertions.assertEquals(new BigDecimal("60.00"), share1.getAmount());

        ExpenseShare share2 = shares.stream().filter(s -> s.getUser().equals(user2)).findFirst().orElseThrow();
        Assertions.assertEquals(new BigDecimal("40.00"), share2.getAmount());
    }

    @Test
    @DisplayName("Should handle rounding with remainder")
    void testPercentageSplitWithRemainder() {
        // 100.00 split 33.33%, 33.33%, 33.34% sum is 100.
        // But 33.33% of 100 is 33.33.
        // Let's try tricky one: 50.00 Amount, split 33.00, 33.00, 34.00 percents.
        // 50 * 0.33 = 16.50
        // 50 * 0.33 = 16.50
        // 50 * 0.34 = 17.00
        // Sum = 50.00. Correct.
        
        // What about 100.01 amount? 50%, 50%.
        // 50.005 -> 50.01 (Round HALF_UP)
        // 50.005 -> 50.01
        // Sum = 100.02. Mismatch! Logic should fix this.
        
        Expense expense = Expense.builder()
                .amount(new BigDecimal("100.01"))
                .build();
        
        List<User> participants = Arrays.asList(user1, user2);
        Map<Long, BigDecimal> percentages = new HashMap<>();
        percentages.put(1L, new BigDecimal("50.00"));
        percentages.put(2L, new BigDecimal("50.00"));
        
        // strategy calcs: 100.01 * 0.50 = 50.005 -> 50.01
        // sum = 100.02.
        // Logic subtracts remainder (100.01 - 100.02 = -0.01).
        // First share becomes 50.01 + (-0.01) = 50.00.
        // Second share 50.01.
        // Sum = 100.01. Correct.
        
        List<ExpenseShare> shares = strategy.split(expense, participants, percentages);
        BigDecimal sum = shares.stream().map(ExpenseShare::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Assertions.assertEquals(expense.getAmount(), sum);
    }
    
    @Test
    @DisplayName("Should throw exception if percentages do not sum to 100")
    void testInvalidPercentageSum() {
        Expense expense = Expense.builder().amount(new BigDecimal("100.00")).build();
        List<User> participants = Arrays.asList(user1, user2);
        Map<Long, BigDecimal> percentages = new HashMap<>();
        percentages.put(1L, new BigDecimal("50.00"));
        percentages.put(2L, new BigDecimal("49.00")); // 99%

        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            strategy.split(expense, participants, percentages)
        );
    }
}
