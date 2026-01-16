package com.splitwise.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.splitwise.dto.ExpenseResponseDTO;
import com.splitwise.entity.Expense;
import com.splitwise.entity.ExpenseShare;
import com.splitwise.entity.User;
import com.splitwise.enums.SplitType;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.ExpenseShareRepository;
import com.splitwise.repository.GroupRepository;
import com.splitwise.repository.UserRepository;
import com.splitwise.strategy.SplitStrategy;
import com.splitwise.strategy.SplitStrategyFactory;
import com.splitwise.validator.ExpenseValidator;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpenseShareRepository expenseShareRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserBalanceService userBalanceService;
    @Mock
    private SplitStrategyFactory splitStrategyFactory;
    @Mock
    private SplitStrategy splitStrategy;
    @Mock
    private ExpenseValidator expenseValidator;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    @DisplayName("Should create expense successfully")
    void testCreateExpense() {
        Long payerId = 1L;
        String description = "Dinner";
        BigDecimal amount = new BigDecimal("100.00");
        SplitType splitType = SplitType.EQUAL;
        List<Long> participantIds = Arrays.asList(1L, 2L);

        User payer = User.builder().id(payerId).name("Payer").email("payer@test.com").build();
        User participant2 = User.builder().id(2L).name("P2").email("p2@test.com").build();
        List<User> participants = Arrays.asList(payer, participant2);

        Mockito.when(userRepository.findById(payerId)).thenReturn(Optional.of(payer));
        Mockito.when(userRepository.findAllById(participantIds)).thenReturn(participants);
        
        Expense savedExpense = Expense.builder()
                .id(1L)
                .description(description)
                .amount(amount)
                .splitType(splitType)
                .paidBy(payer)
                .createdAt(LocalDateTime.now())
                .shares(new java.util.ArrayList<>())
                .build();

        Mockito.when(expenseRepository.save(Mockito.any(Expense.class))).thenReturn(savedExpense);
        Mockito.when(splitStrategyFactory.getStrategy(splitType)).thenReturn(splitStrategy);
        
        ExpenseShare share1 = ExpenseShare.builder().id(1L).user(payer).amount(new BigDecimal("50.00")).settled(false).build();
        ExpenseShare share2 = ExpenseShare.builder().id(2L).user(participant2).amount(new BigDecimal("50.00")).settled(false).build();
        List<ExpenseShare> shares = Arrays.asList(share1, share2);
        
        Mockito.when(splitStrategy.split(Mockito.any(Expense.class), Mockito.eq(participants), Mockito.any()))
                .thenReturn(shares);

        ExpenseResponseDTO result = expenseService.createExpense(payerId, null, description, amount, splitType, participantIds, null);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(amount, result.getAmount());
        Assertions.assertEquals(2, result.getShares().size());
        
        Mockito.verify(expenseValidator).validateAndThrow(Mockito.any(), Mockito.anyString());
        Mockito.verify(expenseRepository).save(Mockito.any(Expense.class));
        Mockito.verify(splitStrategy).split(Mockito.any(Expense.class), Mockito.eq(participants), Mockito.any());
        Mockito.verify(expenseShareRepository, Mockito.times(2)).save(Mockito.any(ExpenseShare.class));
        Mockito.verify(userBalanceService, Mockito.times(2)).updateUserBalance(Mockito.any(), Mockito.any(), Mockito.any());
    }
}
