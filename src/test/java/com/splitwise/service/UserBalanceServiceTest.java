package com.splitwise.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.splitwise.dto.BalanceResponseDTO;
import com.splitwise.entity.User;
import com.splitwise.entity.UserBalance;
import com.splitwise.repository.UserBalanceRepository;
import com.splitwise.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserBalanceServiceTest {

    @Mock
    private UserBalanceRepository userBalanceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserBalanceService userBalanceService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).name("Alice").email("alice@test.com").build();
        bob = User.builder().id(2L).name("Bob").email("bob@test.com").build();
    }

    @Test
    void testUpdateUserBalance_NewBalance() {
        Mockito.when(userBalanceRepository.findByFromUserAndToUser(alice, bob)).thenReturn(Optional.empty());

        userBalanceService.updateUserBalance(alice, bob, new BigDecimal("10.00"));

        Mockito.verify(userBalanceRepository).save(Mockito.argThat(balance -> 
            balance.getFromUser().equals(alice) && 
            balance.getToUser().equals(bob) && 
            balance.getBalance().compareTo(new BigDecimal("-10.00")) == 0
        ));
    }

    @Test
    void testUpdateUserBalance_ExistingBalance() {
        UserBalance existing = UserBalance.builder()
                .fromUser(alice)
                .toUser(bob)
                .balance(new BigDecimal("5.00"))
                .build();
        Mockito.when(userBalanceRepository.findByFromUserAndToUser(alice, bob)).thenReturn(Optional.of(existing));

        userBalanceService.updateUserBalance(alice, bob, new BigDecimal("10.00"));

        Mockito.verify(userBalanceRepository).save(Mockito.argThat(balance -> 
            balance.getBalance().compareTo(new BigDecimal("-5.00")) == 0
        ));
    }

    @Test
    void testUpdateUserBalance_SelfNotAllowed() {
        userBalanceService.updateUserBalance(alice, alice, new BigDecimal("10.00"));
        Mockito.verifyNoInteractions(userBalanceRepository);
    }

    @Test
    void testGetUserBalance_Consolidated() {
        UserBalance b1 = UserBalance.builder()
                .fromUser(alice) // Alice owes Bob if balance > 0
                .toUser(bob)
                .balance(new BigDecimal("30.00")) // Alice owes Bob 30
                .build();

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        Mockito.when(userBalanceRepository.findAllByFromUserOrToUser(alice, alice)).thenReturn(List.of(b1));

        BalanceResponseDTO result = userBalanceService.getUserBalance(1L);

        Assertions.assertEquals(1, result.getOwedByUser().size());
        Assertions.assertEquals(0, result.getOwedToUser().size());
        Assertions.assertEquals(bob.getId(), result.getOwedByUser().get(0).getUser().getId());
        Assertions.assertEquals(new BigDecimal("30.00"), result.getOwedByUser().get(0).getAmount());
    }

    @Test
    void testGetUserBalance_UserNotFound() {
        Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());
        Assertions.assertThrows(EntityNotFoundException.class, () -> userBalanceService.getUserBalance(99L));
    }
}
