package com.splitwise.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.User;

/**
 * Strategy implementation for splitting expenses using percentages per participant.
 * Validates that the sum of percentages equals 100%.
 */
@Component
public class PercentageSplitStrategy implements SplitStrategy {
    
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal ERROR_MARGIN = new BigDecimal("0.01");

    @Override
    public List<ExpenseShare> split(
            Expense expense,
            List<User> participants,
            Map<Long, BigDecimal> percentages
    ) {
        validate(expense, participants, percentages);

        List<ExpenseShare> shares = new ArrayList<>();
        BigDecimal totalAmount = expense.getAmount();

        for (User participant : participants) {
            BigDecimal percentage = percentages.get(participant.getId());
            
            // Calculate share: (Total * Percentage) / 100
            BigDecimal shareAmount = totalAmount.multiply(percentage)
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            
            // Note: Simple multiplication might cause rounding issues if not careful.
            // But usually 33.33% of 100 is 33.33. 
            // If we have 33.33, 33.33, 33.34 -> 99.99 for sum. 
            // Better to re-check sum of shares later, but users provide PERCENTAGES that sum to 100.
            // If user provides 33.33, 33.33, 33.33 = 99.99 total percentage. We rely on validation.
            // In strict mode, we could distribute rounding errors, 
            // but for percentage, getting "close enough" and checking sum is standard.
            
            ExpenseShare share = ExpenseShare.builder()
                    .expense(expense)
                    .user(participant)
                    .amount(shareAmount)
                    .settled(false)
                    .build();

            shares.add(share);
        }
        
        // Final sanity check on share sum? 
        // If 100 total, 33.33, 33.33, 33.33 (users gave 33.33 each) -> sum shares = 99.99. 
        // We miss 1 cent.
        // We should fix this similar to EqualSplit.
        
        BigDecimal currentSum = shares.stream()
                .map(ExpenseShare::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        if (currentSum.compareTo(totalAmount) != 0) {
            BigDecimal remainder = totalAmount.subtract(currentSum);
            // Add remainder to first share? Or the one with highest percentage? 
            // Let's add to first share for simplicity or random.
            if (!shares.isEmpty()) {
                 ExpenseShare firstShare = shares.get(0);
                 firstShare.setAmount(firstShare.getAmount().add(remainder));
            }
        }

        return shares;
    }

    @Override
    public void validate(
            Expense expense,
            List<User> participants,
            Map<Long, BigDecimal> percentages
    ) {
        // Call default validation first
        SplitStrategy.super.validate(expense, participants, percentages);

        if (percentages == null || percentages.isEmpty()) {
            throw new IllegalArgumentException(
                    "Percentages map is required for PERCENTAGE split type"
            );
        }

        // Validate all participants have a percentage
        for (User participant : participants) {
            if (!percentages.containsKey(participant.getId())) {
                throw new IllegalArgumentException(
                        "Missing percentage for participant: " + participant.getId()
                );
            }
            
            BigDecimal percentage = percentages.get(participant.getId());
             if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Invalid percentage for participant: " + participant.getId()
                );
            }
        }

        // Validate sum of percentages equals 100
        BigDecimal sumOfPercentages = percentages.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Allow small margin? No, users should provide exactly 100 for explicit percentage.
        // Usually UI helps sum to 100.
        if (sumOfPercentages.compareTo(ONE_HUNDRED) != 0) {
             throw new IllegalArgumentException(
                    String.format(
                            "Sum of percentages (%s) does not equal 100%%",
                            sumOfPercentages
                    )
            );
        }
    }
}
