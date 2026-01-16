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

import lombok.extern.slf4j.Slf4j;

/**
 * Strategy implementation for splitting expenses using percentages per participant.
 * Validates that the sum of percentages equals 100%. Handles precision adjustments.
 */
@Component
@Slf4j
public class PercentageSplitStrategy implements SplitStrategy {
    
    private static final BigDecimal PERCENTAGE_DIVISOR = new BigDecimal("100.00");

    @Override
    public List<ExpenseShare> split(
            Expense expense,
            List<User> participants,
            Map<Long, BigDecimal> percentages
    ) {
        validate(expense, participants, percentages);

        List<ExpenseShare> shares = new ArrayList<>();
        BigDecimal totalAmount = expense.getAmount();
        log.debug("Splitting expense amount {} using percentages for {} participants", totalAmount, participants.size());

        for (User participant : participants) {
            BigDecimal percentage = percentages.get(participant.getId());
            
            // Calculate share: (Total * Percentage) / 100
            BigDecimal shareAmount = totalAmount.multiply(percentage)
                    .divide(PERCENTAGE_DIVISOR, 2, RoundingMode.HALF_UP);
            
            log.trace("Participant {}: percentage {}%, calculated share {}", participant.getId(), percentage, shareAmount);
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
            log.info("Percentages caused rounding gap of {}. Adjusting first participant.", remainder);
            
            if (!shares.isEmpty()) {
                 ExpenseShare firstShare = shares.get(0);
                 BigDecimal adjustedAmount = firstShare.getAmount().add(remainder);
                 log.debug("Adjusting share for user {} from {} to {}", firstShare.getUser().getId(), firstShare.getAmount(), adjustedAmount);
                 firstShare.setAmount(adjustedAmount);
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

        if (sumOfPercentages.compareTo(PERCENTAGE_DIVISOR) != 0) {
             throw new IllegalArgumentException(
                    String.format(
                            "Sum of percentages (%s) does not equal 100%%",
                            sumOfPercentages
                    )
            );
        }
    }
}
