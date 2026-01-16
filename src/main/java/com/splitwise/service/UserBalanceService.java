package com.splitwise.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitwise.dto.BalanceResponseDTO;
import com.splitwise.dto.UserSummaryDTO;
import com.splitwise.entity.User;
import com.splitwise.entity.UserBalance;
import com.splitwise.repository.UserBalanceRepository;
import com.splitwise.repository.UserRepository;
import com.splitwise.util.StructuredLogging;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBalanceService {

    private final UserBalanceRepository userBalanceRepository;
    private final UserRepository userRepository;

    /**
     * Updates the balance between a payer and a debtor.
     * Guaranteed to maintain the unique constraint (fromUser.id < toUser.id).
     *
     * @param payer  the user who paid (is owed money)
     * @param debtor the user who owes money
     * @param amount the amount owed
     */
    @Transactional
    public void updateUserBalance(User payer, User debtor, BigDecimal amount) {
        if (payer.getId().equals(debtor.getId())) {
            log.trace("Skipping balance update for self: {}", StructuredLogging.getKV("userId", payer.getId()));
            return; 
        }

        User fromUser;
        User toUser;
        BigDecimal amountAdjustment;

        // Ensure fromUser always has the lower ID to satisfy unique constraint
        if (payer.getId() < debtor.getId()) {
            fromUser = payer;
            toUser = debtor;
            amountAdjustment = amount.negate();
        } else {
            fromUser = debtor;
            toUser = payer;
            amountAdjustment = amount;
        }

        log.debug("Updating balance: pair=[{}, {}], {}", 
                fromUser.getId(), toUser.getId(), StructuredLogging.getKV("adjustment", amountAdjustment));

        Optional<UserBalance> existingBalance = userBalanceRepository
                .findByFromUserAndToUser(fromUser, toUser);

        UserBalance userBalance = existingBalance.orElseGet(() -> {
            UserBalance newBalance = UserBalance.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .balance(BigDecimal.ZERO)
                .build();
            log.info("Updating user balance: {}", StructuredLogging.getKV("balanceId", newBalance.getId()));
            return newBalance;
        });

        userBalance.addToBalance(amountAdjustment);
        userBalanceRepository.save(userBalance);
    }

    /**
     * Gets the consolidated view of balances for a user.
     *
     * @param userId the ID of the user
     * @return BalanceResponseDTO containing owedToUser and owedByUser
     */
    @Transactional(readOnly = true)
    public BalanceResponseDTO getUserBalance(Long userId) {
        log.debug("Fetching balances for {}", StructuredLogging.getKV("userId", userId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<UserBalance> balances = userBalanceRepository.findAllByFromUserOrToUser(user, user);

        List<BalanceResponseDTO.UserBalanceDTO> owedToUser = new ArrayList<>();
        List<BalanceResponseDTO.UserBalanceDTO> owedByUser = new ArrayList<>();

        for (UserBalance b : balances) {
            BigDecimal amount = b.getBalance();
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;

            User fromUser = b.getFromUser();
            User toUser = b.getToUser();

            User creditor;
            User debtor;
            BigDecimal absoluteAmount = amount.abs();

            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                debtor = fromUser;
                creditor = toUser;
            } else {
                debtor = toUser;
                creditor = fromUser;
            }

            if (creditor.getId().equals(userId)) {
                owedToUser.add(BalanceResponseDTO.UserBalanceDTO.builder()
                        .user(toSummary(debtor))
                        .amount(absoluteAmount)
                        .build());
            } else if (debtor.getId().equals(userId)) {
                owedByUser.add(BalanceResponseDTO.UserBalanceDTO.builder()
                        .user(toSummary(creditor))
                        .amount(absoluteAmount)
                        .build());
            }
        }

        log.debug("Found {} credit/debit pairs for {}", 
                owedToUser.size() + owedByUser.size(), StructuredLogging.getKV("userId", userId));
        return BalanceResponseDTO.builder()
                .owedToUser(owedToUser)
                .owedByUser(owedByUser)
                .build();
    }

    private UserSummaryDTO toSummary(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
