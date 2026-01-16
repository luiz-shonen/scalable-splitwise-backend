package com.splitwise.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BalanceResponseDTO {
    private List<UserBalanceDTO> owedToUser;
    private List<UserBalanceDTO> owedByUser;

    @Data
    @Builder
    public static class UserBalanceDTO {
        private UserSummaryDTO user;
        private BigDecimal amount;
    }
}
