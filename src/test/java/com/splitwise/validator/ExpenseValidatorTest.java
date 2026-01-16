package com.splitwise.validator;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.splitwise.dto.ExpenseValidationContext;
import com.splitwise.entity.Group;
import com.splitwise.entity.User;

class ExpenseValidatorTest {

    private final ExpenseValidator expenseValidator = new ExpenseValidator();
    
    private User alice;
    private User bob;
    private Group housemates;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).name("Alice").build();
        bob = User.builder().id(2L).name("Bob").build();
        housemates = Group.builder()
                .id(10L)
                .name("Housemates")
                .members(Set.of(alice, bob))
                .build();
    }

    @Test
    void testValidatePositive() {
        ExpenseValidationContext context = ExpenseValidationContext.builder()
                .payer(alice)
                .group(housemates)
                .participants(List.of(alice, bob))
                .build();

        Errors errors = new BeanPropertyBindingResult(context, "context");
        expenseValidator.validate(context, errors);

        Assertions.assertFalse(errors.hasErrors());
    }

    @Test
    void testValidatePayerNotInGroup() {
        User stranger = User.builder().id(99L).name("Stranger").build();
        ExpenseValidationContext context = ExpenseValidationContext.builder()
                .payer(stranger)
                .group(housemates)
                .participants(List.of(alice))
                .build();

        Errors errors = new BeanPropertyBindingResult(context, "context");
        expenseValidator.validate(context, errors);

        Assertions.assertTrue(errors.hasErrors());
        Assertions.assertTrue(errors.getFieldError("payer").getDefaultMessage().contains("Payer with ID 99 does not belong to group"));
    }

    @Test
    void testValidateParticipantNotInGroup() {
        User stranger = User.builder().id(99L).name("Stranger").build();
        ExpenseValidationContext context = ExpenseValidationContext.builder()
                .payer(alice)
                .group(housemates)
                .participants(List.of(stranger))
                .build();

        Errors errors = new BeanPropertyBindingResult(context, "context");
        expenseValidator.validate(context, errors);

        Assertions.assertTrue(errors.hasErrors());
        Assertions.assertTrue(errors.getFieldError("participants").getDefaultMessage().contains("Participant with ID 99 does not belong to group"));
    }
}
