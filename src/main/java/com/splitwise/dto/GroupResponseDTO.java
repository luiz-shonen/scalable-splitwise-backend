package com.splitwise.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupResponseDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private UserSummaryDTO createdBy;
    private List<UserSummaryDTO> members;
    private List<ExpenseSummaryDTO> expenses;

    @Data
    @Builder
    public static class ExpenseSummaryDTO {
        private Long id;
        private String description;
        private BigDecimal amount;
        private LocalDateTime createdAt;
    }
}
