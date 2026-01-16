package com.splitwise.validator;

import java.math.BigDecimal;
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

    private static final String SPLIT_DETAILS_FIELD = "splitDetails";

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

        validateSplitConsistency(context, errors);
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

    private void validateSplitConsistency(ExpenseValidationContext context, Errors errors) {
        if (context.getSplitType() == null) return;

        switch (context.getSplitType()) {
            case EQUAL -> {
                if (context.getSplitDetails() != null && !context.getSplitDetails().isEmpty()) {
                    errors.rejectValue(SPLIT_DETAILS_FIELD, "split.details.redundant", 
                        "Split details should not be provided for EQUAL split type");
                }
            }
            case EXACT -> validateExactSplit(context, errors);
            case PERCENTAGE -> {
                // Future enhancement if needed
            }
        }

        validateParticipantConsistency(context, errors);
    }

    private void validateExactSplit(ExpenseValidationContext context, Errors errors) {
        if (context.getSplitDetails() == null || context.getSplitDetails().isEmpty()) {
            errors.rejectValue(SPLIT_DETAILS_FIELD, "split.details.missing", 
                "Split details are required for EXACT split type");
            return;
        }

        BigDecimal sum = context.getSplitDetails().stream()
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(context.getAmount()) != 0) {
            errors.rejectValue(SPLIT_DETAILS_FIELD, "split.sum.mismatch", 
                String.format("The sum of split details ([%s]) must equal the total expense amount ([%s])", 
                    sum.toPlainString(), context.getAmount().toPlainString()));
        }
    }

    private void validateParticipantConsistency(ExpenseValidationContext context, Errors errors) {
        if (context.getSplitDetails() == null || context.getSplitDetails().isEmpty()) return;

        Set<Long> participantIds = context.getParticipants().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        for (var detail : context.getSplitDetails()) {
            if (!participantIds.contains(detail.getUserId())) {
                errors.rejectValue(SPLIT_DETAILS_FIELD, "split.participant.mismatch", 
                    "User with ID " + detail.getUserId() + " in split details is not in the participants list");
            }
        }
    }
}
