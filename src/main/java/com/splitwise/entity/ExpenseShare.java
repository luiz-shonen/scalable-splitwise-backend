package com.splitwise.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a user's share of an expense.
 * Each share indicates how much a specific user owes for a specific expense.
 */
@Entity
@Table(name = "expense_shares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The amount this user owes for the expense.
     * Uses BigDecimal for monetary precision.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Indicates whether this share has been settled.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean settled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    /**
     * The expense this share belongs to.
     * JsonBackReference to prevent infinite recursion during serialization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    @JsonBackReference(value = "expense-shares")
    private Expense expense;

    /**
     * The user who owes this share.
     * JsonBackReference to prevent infinite recursion during serialization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference(value = "user-shares")
    private User user;

    /**
     * Marks this share as settled and records the settlement time.
     */
    public void settle() {
        this.settled = true;
        this.settledAt = LocalDateTime.now();
    }
}
