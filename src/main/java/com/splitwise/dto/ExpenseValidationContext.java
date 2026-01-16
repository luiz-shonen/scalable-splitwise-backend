package com.splitwise.dto;

import java.math.BigDecimal;
import java.util.List;

import com.splitwise.entity.Group;
import com.splitwise.entity.User;
import com.splitwise.enums.SplitType;

import lombok.Builder;
import lombok.Data;

/**
 * Context object holding all data needed for expense validation.
 */
@Data
@Builder
public class ExpenseValidationContext {
    private User payer;
    private Group group;
    private List<User> participants;
    private BigDecimal amount;
    private SplitType splitType;
    private List<ExpenseSplitDTO> splitDetails;
}
