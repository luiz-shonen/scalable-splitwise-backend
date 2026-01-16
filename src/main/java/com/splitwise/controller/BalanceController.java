package com.splitwise.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitwise.dto.BalanceResponseDTO;
import com.splitwise.dto.UserSummaryDTO;
import com.splitwise.entity.User;
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
     * Returns a DTO containing who owes this user and who this user owes.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<BalanceResponseDTO> getUserBalances(@PathVariable(name = "userId") Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found");
        }

        List<UserBalance> allBalances = userBalanceRepository.findAll();

        List<BalanceResponseDTO.UserBalanceDTO> owedToUser = new ArrayList<>();
        List<BalanceResponseDTO.UserBalanceDTO> owedByUser = new ArrayList<>();

        for (UserBalance b : allBalances) {
            if (b.getBalance().compareTo(BigDecimal.ZERO) == 0) continue;

            if (b.getToUser().getId().equals(userId)) {
                owedToUser.add(BalanceResponseDTO.UserBalanceDTO.builder()
                        .user(toSummary(b.getFromUser()))
                        .amount(b.getBalance())
                        .build());
            } else if (b.getFromUser().getId().equals(userId)) {
                owedByUser.add(BalanceResponseDTO.UserBalanceDTO.builder()
                        .user(toSummary(b.getToUser()))
                        .amount(b.getBalance())
                        .build());
            }
        }

        return ResponseEntity.ok(BalanceResponseDTO.builder()
                .owedToUser(owedToUser)
                .owedByUser(owedByUser)
                .build());
    }

    private UserSummaryDTO toSummary(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
