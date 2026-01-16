package com.splitwise.service;

import com.splitwise.entity.User;
import com.splitwise.entity.UserBalance;
import com.splitwise.repository.UserBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserBalanceService {

    private final UserBalanceRepository userBalanceRepository;

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
            return; // No balance update needed for self-owed amounts
        }

        User fromUser;
        User toUser;
        BigDecimal amountAdjustment;

        // Ensure fromUser always has the lower ID to satisfy unique constraint
        if (payer.getId() < debtor.getId()) {
            // Payer (from) < Debtor (to)
            // We want to record that Debtor owes Payer.
            // Since Balance positive means "From owes To", and here "To owes From",
            // we must subtract the amount.
            // Example: Payer=1 (from), Debtor=2 (to). Debtor owes Payer.
            // Balance represents 1 owes 2. Since 2 owes 1, Balance decreases.
            fromUser = payer;
            toUser = debtor;
            amountAdjustment = amount.negate();
        } else {
            // Debtor (from) < Payer (to)
            // Debtor owes Payer.
            // Balance positive means "From (Debtor) owes To (Payer)".
            // So we add the amount.
            fromUser = debtor;
            toUser = payer;
            amountAdjustment = amount;
        }

        Optional<UserBalance> existingBalance = userBalanceRepository
                .findByFromUserAndToUser(fromUser, toUser);

        UserBalance userBalance = existingBalance.orElseGet(() -> UserBalance.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .balance(BigDecimal.ZERO)
                .build());

        userBalance.addToBalance(amountAdjustment);
        userBalanceRepository.save(userBalance);
    }
}
