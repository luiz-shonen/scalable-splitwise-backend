package com.splitwise.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitwise.dto.ExpenseResponseDTO;
import com.splitwise.dto.UserSummaryDTO;
import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.enums.SplitType;
import com.splitwise.exception.ValidationException;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.ExpenseShareRepository;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;
import com.splitwise.strategy.SplitStrategy;
import com.splitwise.strategy.SplitStrategyFactory;

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
            Map<Long, BigDecimal> exactAmounts
    ) {
        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new EntityNotFoundException("Payer not found: " + payerId));

        Group group = null;
        if (groupId != null) {
            group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));
            
            // Validate Payer belongs to Group
            if (!group.getMembers().contains(payer)) {
                throw new ValidationException("Payer with ID " + payerId + " does not belong to group: " + group.getName());
            }
        }

        List<User> participants = userRepository.findAllById(participantIds);
        if (participants.size() != participantIds.size()) {
            throw new EntityNotFoundException("One or more participants not found");
        }

        // Validate all participants belong to the group if groupId is present
        if (group != null) {
            Set<Long> groupMemberIds = group.getMembers().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());
            
            for (User participant : participants) {
                if (!groupMemberIds.contains(participant.getId())) {
                    throw new ValidationException("Participant with ID " + participant.getId() + " does not belong to group: " + group.getName());
                }
            }
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
