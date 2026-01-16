package com.splitwise.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitwise.entity.UserBalance;
import com.splitwise.repository.UserBalanceRepository;
import com.splitwise.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final UserBalanceRepository userBalanceRepository;
    private final UserRepository userRepository;

    /**
     * Gets the consolidated view of balances for a user.
     * Returns a map of who owes this user and who this user owes.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBalances(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found");
        }

        // Fetch all balances to calculate consolidated view
        // Note: For large datasets, a custom SQL query using aggregation would be more performant
        List<UserBalance> allBalances = userBalanceRepository.findAll();

        List<Map<String, Object>> owedToUser = new ArrayList<>();
        List<Map<String, Object>> owedByUser = new ArrayList<>();

        for (UserBalance b : allBalances) {
            if (b.getBalance().compareTo(BigDecimal.ZERO) == 0) continue;

            // Recall: Balance positive means From owes To.
            // b.getFromUser() owes b.getToUser() amount b.getBalance()

            if (b.getToUser().getId().equals(userId)) {
                // Someone owes this user (Positive balance)
                Map<String, Object> entry = new HashMap<>();
                entry.put("user", b.getFromUser());
                entry.put("amount", b.getBalance());
                owedToUser.add(entry);
            } else if (b.getFromUser().getId().equals(userId)) {
                // This user owes someone (Positive balance)
                Map<String, Object> entry = new HashMap<>();
                entry.put("user", b.getToUser());
                entry.put("amount", b.getBalance());
                owedByUser.add(entry);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("owedToUser", owedToUser);
        response.put("owedByUser", owedByUser);

        return ResponseEntity.ok(response);
    }
}
