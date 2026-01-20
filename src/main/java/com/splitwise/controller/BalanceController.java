package com.splitwise.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitwise.dto.BalanceResponseDTO;
import com.splitwise.dto.SettleRequestDTO;
import com.splitwise.service.UserBalanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    /**
     * Settles a payment between two users.
     */
    @PostMapping("/settle")
    @Operation(summary = "Settle a payment", description = "Records a payment between two users to reduce their debt.")
    @ApiResponse(responseCode = "200", description = "Payment settled successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input or validation error")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> settlePayment(@Valid @RequestBody SettleRequestDTO request) {
        userBalanceService.settlePayment(
                request.getFromUserId(),
                request.getToUserId(),
                request.getAmount()
        );
        return ResponseEntity.ok().build();
    }
}
