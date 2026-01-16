package com.splitwise.strategy;

import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy implementation for splitting expenses using exact amounts per participant.
 * Validates that the sum of exact amounts equals the total expense amount.
 */
@Component
public class ExactAmountStrategy implements SplitStrategy {

    @Override
    public List<ExpenseShare> split(
            Expense expense,
            List<User> participants,
            Map<Long, BigDecimal> exactAmounts
    ) {
        validate(expense, participants, exactAmounts);

        List<ExpenseShare> shares = new ArrayList<>();

        for (User participant : participants) {
            BigDecimal shareAmount = exactAmounts.get(participant.getId());

            if (shareAmount == null) {
                throw new IllegalArgumentException(
                        "Exact amount not provided for user: " + participant.getId()
                );
            }

            ExpenseShare share = ExpenseShare.builder()
                    .expense(expense)
                    .user(participant)
                    .amount(shareAmount)
                    .settled(false)
                    .build();

            shares.add(share);
        }

        return shares;
    }

    @Override
    public void validate(
            Expense expense,
            List<User> participants,
            Map<Long, BigDecimal> exactAmounts
    ) {
        // Call default validation first
        SplitStrategy.super.validate(expense, participants, exactAmounts);

        if (exactAmounts == null || exactAmounts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Exact amounts map is required for EXACT split type"
            );
        }

        // Validate all participants have an amount
        for (User participant : participants) {
            if (!exactAmounts.containsKey(participant.getId())) {
                throw new IllegalArgumentException(
                        "Missing exact amount for participant: " + participant.getId()
                );
            }

            BigDecimal amount = exactAmounts.get(participant.getId());
            if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Invalid amount for participant: " + participant.getId()
                );
            }
        }

        // Validate sum of exact amounts equals total expense
        BigDecimal sumOfExactAmounts = exactAmounts.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumOfExactAmounts.compareTo(expense.getAmount()) != 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Sum of exact amounts (%s) does not equal expense total (%s)",
                            sumOfExactAmounts,
                            expense.getAmount()
                    )
            );
        }
    }
}
