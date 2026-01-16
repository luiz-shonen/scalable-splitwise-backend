package com.splitwise.validator;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import com.splitwise.dto.ExpenseValidationContext;
import com.splitwise.entity.User;

/**
 * Specialist validator for Expenses following Spring's SmartValidator interface.
 * Validates group membership and business logic consistency with support for validation hints.
 */
@Component
public class ExpenseValidator implements BaseValidator {

    @Override
    public boolean supports(Class<?> clazz) {
        return ExpenseValidationContext.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        validate(target, errors, (Object[]) null);
    }

    @Override
    public void validate(Object target, Errors errors, Object... validationHints) {
        ExpenseValidationContext context = (ExpenseValidationContext) target;

        if (context.getGroup() != null) {
            validateGroupMembership(context, errors);
        }
    }

    private void validateGroupMembership(ExpenseValidationContext context, Errors errors) {
        Set<Long> groupMemberIds = context.getGroup().getMembers().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // Validate Payer
        if (!groupMemberIds.contains(context.getPayer().getId())) {
            errors.rejectValue("payer", "group.membership.invalid", 
                "Payer with ID " + context.getPayer().getId() + " does not belong to group: " + context.getGroup().getName());
        }

        // Validate Participants
        for (User participant : context.getParticipants()) {
            if (!groupMemberIds.contains(participant.getId())) {
                errors.rejectValue("participants", "group.membership.invalid", 
                    "Participant with ID " + participant.getId() + " does not belong to group: " + context.getGroup().getName());
            }
        }
    }
}
