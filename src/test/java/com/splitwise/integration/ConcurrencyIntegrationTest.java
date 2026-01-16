package com.splitwise.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.splitwise.entity.User;
import com.splitwise.entity.UserBalance;
import com.splitwise.repository.UserBalanceRepository;
import com.splitwise.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyIntegrationTest {

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Should fail with OptimisticLockingFailureException when concurrent updates occur")
    void testOptimisticLockingOnUserBalance() throws InterruptedException, ExecutionException {
        // Setup users and initial balance
        User user1 = userRepository.save(User.builder().name("User 1").email("u1@test.com").build());
        User user2 = userRepository.save(User.builder().name("User 2").email("u2@test.com").build());
        
        UserBalance balance = userBalanceRepository.save(UserBalance.builder()
                .fromUser(user1)
                .toUser(user2)
                .balance(BigDecimal.ZERO)
                .build());

        Long balanceId = balance.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Load, wait, update
        Future<?> future1 = executor.submit(() -> {
            transactionTemplate.execute(status -> {
                UserBalance b1 = userBalanceRepository.findById(balanceId).orElseThrow();
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                b1.addToBalance(new BigDecimal("10.00"));
                userBalanceRepository.save(b1);
                return null;
            });
        });

        // Thread 2: Load, update quickly
        Future<?> future2 = executor.submit(() -> {
            transactionTemplate.execute(status -> {
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                UserBalance b2 = userBalanceRepository.findById(balanceId).orElseThrow();
                b2.addToBalance(new BigDecimal("20.00"));
                userBalanceRepository.save(b2);
                return null;
            });
        });

        future2.get(); // Thread 2 should succeed
        
        // Thread 1 should fail because b1 has an old version
        ExecutionException ex = assertThrows(ExecutionException.class, future1::get);
        assertEquals(ObjectOptimisticLockingFailureException.class, ex.getCause().getClass());

        // Verify final balance is 20.00 (only Thread 2's update survived)
        UserBalance finalBalance = userBalanceRepository.findById(balanceId).orElseThrow();
        assertEquals(0, new BigDecimal("20.00").compareTo(finalBalance.getBalance()));
        
        executor.shutdown();
    }
}
