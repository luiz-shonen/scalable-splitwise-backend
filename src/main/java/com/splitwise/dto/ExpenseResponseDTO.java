package com.splitwise.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.splitwise.enums.SplitType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpenseResponseDTO {
    private Long id;
    private String description;
    private BigDecimal amount;
    private SplitType splitType;
    private LocalDateTime createdAt;
    private UserSummaryDTO paidBy;
    private Long groupId;
    private List<ExpenseShareDTO> shares;

    @Data
    @Builder
    public static class ExpenseShareDTO {
        private Long id;
        private UserSummaryDTO user;
        private BigDecimal amount;
        private boolean settled;
    }
}
