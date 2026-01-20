package com.splitwise.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitwise.dto.CreateExpenseRequest;
import com.splitwise.dto.ExpenseResponseDTO;
import com.splitwise.service.ExpenseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Endpoints for managing expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "Create a new expense", description = "Creates an expense, splits it among participants, and updates user balances.")
    @ApiResponse(responseCode = "200", description = "Expense created successfully", content = @Content(schema = @Schema(implementation = ExpenseResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input or validation error")
    @ApiResponse(responseCode = "404", description = "User or Group not found")
    public ResponseEntity<ExpenseResponseDTO> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        ExpenseResponseDTO expense = expenseService.createExpense(
                request.getPaidById(),
                request.getGroupId(),
                request.getDescription(),
                request.getAmount(),
                request.getSplitType(),
                request.getParticipantIds(),
                request.getSplitDetails()
        );
        return ResponseEntity.ok(expense);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get group expenses", description = "Returns all expenses for a specific group.")
    public ResponseEntity<List<ExpenseResponseDTO>> getGroupExpenses(@PathVariable(name = "groupId") Long groupId) {
        return ResponseEntity.ok(expenseService.getGroupExpenses(groupId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user expenses", description = "Returns all expenses a user is involved in (as payer or participant).")
    public ResponseEntity<List<ExpenseResponseDTO>> getUserExpenses(@PathVariable(name = "userId") Long userId) {
        return ResponseEntity.ok(expenseService.getUserExpenses(userId));
    }
}
