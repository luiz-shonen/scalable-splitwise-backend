package com.splitwise.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitwise.dto.BalanceResponseDTO;
import com.splitwise.service.UserBalanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/balances")
@RequiredArgsConstructor
@Tag(name = "Balances", description = "Operations related to user balances and debts")
public class BalanceController {

    private final UserBalanceService userBalanceService;

    /**
     * Gets the consolidated view of balances for a user.
     * Delegates all logic to the Service Layer.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user balances", description = "Returns who owes money to the user and who the user owes.")
    public ResponseEntity<BalanceResponseDTO> getUserBalances(@PathVariable(name = "userId") Long userId) {
        return ResponseEntity.ok(userBalanceService.getUserBalance(userId));
    }
}
