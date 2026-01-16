package com.splitwise.service;

import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.enums.SplitType;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.ExpenseShareRepository;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;
import com.splitwise.strategy.SplitStrategy;
import com.splitwise.strategy.SplitStrategyFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserBalanceService userBalanceService;
    private final SplitStrategyFactory splitStrategyFactory;

    /**
     * Creates a new expense, splits it among participants, and updates user balances.
     *
     * @param payerId       ID of the user who paid
     * @param groupId       ID of the group (optional)
     * @param description   Description of the expense
     * @param amount        Total amount
     * @param splitType     Strategy to use for splitting
     * @param participantIds List of user IDs to split among
     * @param exactAmounts  Map of exact amounts (required if splitType is EXACT)
     * @return The created Expense
     */
    @Transactional
    public Expense createExpense(
            Long payerId,
            Long groupId,
            String description,
            BigDecimal amount,
            SplitType splitType,
            List<Long> participantIds,
            Map<Long, BigDecimal> exactAmounts
    ) {
        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new EntityNotFoundException("Payer not found: " + payerId));

        Group group = null;
        if (groupId != null) {
            group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));
        }

        List<User> participants = userRepository.findAllById(participantIds);
        if (participants.size() != participantIds.size()) {
            throw new EntityNotFoundException("One or more participants not found");
        }

        // 1. Create Expense
        Expense expense = Expense.builder()
                .description(description)
                .amount(amount)
                .splitType(splitType)
                .paidBy(payer)
                .group(group)
                .build();

        expense = expenseRepository.save(expense);

        // 2. Calculate Shares using Strategy
        SplitStrategy strategy = splitStrategyFactory.getStrategy(splitType);
        List<ExpenseShare> shares = strategy.split(expense, participants, exactAmounts);

        // 3. Save Shares and Update Balances
        for (ExpenseShare share : shares) {
            share.setExpense(expense);
            expenseShareRepository.save(share);

            // Update balance: Participant owes Payer
            // If participant is the payer, strict logic would say they owe themselves, 
            // but UserBalanceService handles self-owed check.
            userBalanceService.updateUserBalance(payer, share.getUser(), share.getAmount());
        }
        
        expense.setShares(shares);
        return expense;
    }
}
