package com.splitwise.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitwise.dto.ExpenseResponseDTO;
import com.splitwise.dto.ExpenseSplitDTO;
import com.splitwise.dto.ExpenseValidationContext;
import com.splitwise.dto.UserSummaryDTO;
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
import com.splitwise.validator.ExpenseValidator;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserBalanceService userBalanceService;
    private final SplitStrategyFactory splitStrategyFactory;
    private final ExpenseValidator expenseValidator;

    /**
     * Creates a new expense, splits it among participants, and updates user balances.
     * Guaranteed atomic via @Transactional.
     */
    @Transactional
    public ExpenseResponseDTO createExpense(
            Long payerId,
            Long groupId,
            String description,
            BigDecimal amount,
            SplitType splitType,
            List<Long> participantIds,
            List<ExpenseSplitDTO> splitDetails
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

        ExpenseValidationContext context = ExpenseValidationContext.builder()
                .payer(payer)
                .group(group)
                .participants(participants)
                .amount(amount)
                .splitType(splitType)
                .splitDetails(splitDetails)
                .build();

        expenseValidator.validateAndThrow(context, "expenseValidationContext");

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
        
        Map<Long, BigDecimal> exactAmountsMap = null;
        if (splitDetails != null) {
            exactAmountsMap = splitDetails.stream()
                    .collect(Collectors.toMap(
                            ExpenseSplitDTO::getUserId,
                            ExpenseSplitDTO::getAmount
                    ));
        }

        List<ExpenseShare> shares = strategy.split(expense, participants, exactAmountsMap);

        // 3. Save Shares and Update Balances
        for (ExpenseShare share : shares) {
            share.setExpense(expense);
            expenseShareRepository.save(share);

            // Update balance: Participant owes Payer
            userBalanceService.updateUserBalance(payer, share.getUser(), share.getAmount());
        }
        
        expense.setShares(shares);
        return mapToDTO(expense);
    }

    private ExpenseResponseDTO mapToDTO(Expense expense) {
        return ExpenseResponseDTO.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .splitType(expense.getSplitType())
                .createdAt(expense.getCreatedAt())
                .paidBy(toUserSummary(expense.getPaidBy()))
                .groupId(expense.getGroup() != null ? expense.getGroup().getId() : null)
                .shares(expense.getShares().stream()
                        .map(this::toShareDTO)
                        .toList())
                .build();
    }

    private UserSummaryDTO toUserSummary(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    private ExpenseResponseDTO.ExpenseShareDTO toShareDTO(ExpenseShare share) {
        return ExpenseResponseDTO.ExpenseShareDTO.builder()
                .id(share.getId())
                .user(toUserSummary(share.getUser()))
                .amount(share.getAmount())
                .settled(share.getSettled())
                .build();
    }
}
