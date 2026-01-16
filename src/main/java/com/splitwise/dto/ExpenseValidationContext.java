package com.splitwise.dto;

import java.util.List;

import com.splitwise.entity.Group;
import com.splitwise.entity.User;

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
}
