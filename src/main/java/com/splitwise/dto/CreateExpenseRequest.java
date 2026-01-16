package com.splitwise.dto;

import com.splitwise.enums.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CreateExpenseRequest {
    @NotNull(message = "Payer ID is required")
    private Long paidById;

    private Long groupId;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    @NotEmpty(message = "Participants list cannot be empty")
    private List<Long> participantIds;

    private Map<Long, BigDecimal> exactAmounts;
}
