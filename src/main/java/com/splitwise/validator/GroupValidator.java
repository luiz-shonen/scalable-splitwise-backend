package com.splitwise.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import com.splitwise.entity.Group;

/**
 * Validator for Group domain rules.
 */
@Component
public class GroupValidator implements BaseValidator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Group.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        validate(target, errors, (Object[]) null);
    }

    @Override
    public void validate(Object target, Errors errors, Object... validationHints) {
        Group group = (Group) target;

        if (group.getName() == null || group.getName().trim().isEmpty()) {
            errors.rejectValue("name", "group.name.empty", "Group name cannot be empty");
        }

        if (group.getCreatedBy() == null) {
            errors.rejectValue("createdBy", "group.creator.missing", "Group must have a creator");
        }
    }
}
