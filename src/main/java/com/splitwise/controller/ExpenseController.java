package com.splitwise.controller;

import com.splitwise.dto.CreateExpenseRequest;
import com.splitwise.entity.Expense;
import com.splitwise.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<Expense> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        Expense expense = expenseService.createExpense(
                request.getPaidById(),
                request.getGroupId(),
                request.getDescription(),
                request.getAmount(),
                request.getSplitType(),
                request.getParticipantIds(),
                request.getExactAmounts()
        );
        return ResponseEntity.ok(expense);
    }
}
