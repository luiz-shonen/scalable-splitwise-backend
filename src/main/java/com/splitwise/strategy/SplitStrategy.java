package com.splitwise.strategy;

import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Strategy interface for splitting expenses among participants.
 * Implements the Strategy Pattern to allow different splitting algorithms.
 *
 * <p>This follows the Open/Closed Principle: new split types can be added
 * by implementing this interface without modifying existing code.</p>
 */
public interface SplitStrategy {

    /**
     * Splits an expense among the given participants.
     *
     * @param expense        the expense to split
     * @param participants   the list of users participating in the expense
     * @param exactAmounts   optional map of user IDs to exact amounts (used for EXACT split type)
     * @return a list of ExpenseShare objects representing each participant's share
     * @throws IllegalArgumentException if the split cannot be performed with the given parameters
     */
    List<ExpenseShare> split(
            Expense expense,
            List<User> participants,
            Map<Long, BigDecimal> exactAmounts
    );

    /**
     * Validates that the split can be performed with the given parameters.
     *
     * @param expense        the expense to validate
     * @param participants   the list of users participating
     * @param exactAmounts   optional map of exact amounts
     * @throws IllegalArgumentException if validation fails
     */
    default void validate(
            Expense expense,
            List<User> participants,
            Map<Long, BigDecimal> exactAmounts
    ) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be null or empty");
        }
        if (expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero");
        }
    }
}
