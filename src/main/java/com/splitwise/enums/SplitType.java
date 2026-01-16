package com.splitwise.enums;

/**
 * Enum representing the different strategies for splitting expenses.
 */
public enum SplitType {
    /**
     * Split the expense equally among all participants.
     */
    EQUAL,

    /**
     * Split the expense using exact amounts specified for each participant.
     */
    EXACT,

    /**
     * Split the expense using percentage values for each participant.
     * Reserved for future implementation.
     */
    PERCENTAGE
}
