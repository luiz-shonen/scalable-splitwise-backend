package com.splitwise.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.splitwise.entity.Group;
import com.splitwise.entity.User;

class GroupValidatorTest {

    private final GroupValidator groupValidator = new GroupValidator();

    @Test
    void testValidatePositive() {
        Group group = Group.builder()
                .name("Trip")
                .createdBy(User.builder().id(1L).build())
                .build();

        Errors errors = new BeanPropertyBindingResult(group, "group");
        groupValidator.validate(group, errors);

        Assertions.assertFalse(errors.hasErrors());
    }

    @Test
    void testValidateEmptyName() {
        Group group = Group.builder()
                .name("")
                .createdBy(User.builder().id(1L).build())
                .build();

        Errors errors = new BeanPropertyBindingResult(group, "group");
        groupValidator.validate(group, errors);

        Assertions.assertTrue(errors.hasErrors());
        Assertions.assertEquals("Group name cannot be empty", 
            errors.getFieldError("name").getDefaultMessage());
    }

    @Test
    void testValidateMissingCreator() {
        Group group = Group.builder()
                .name("Trip")
                .build();

        Errors errors = new BeanPropertyBindingResult(group, "group");
        groupValidator.validate(group, errors);

        Assertions.assertTrue(errors.hasErrors());
        Assertions.assertEquals("Group must have a creator", 
            errors.getFieldError("createdBy").getDefaultMessage());
    }
}
