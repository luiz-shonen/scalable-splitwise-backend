package com.splitwise.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.splitwise.enums.SplitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an expense in the Splitwise system.
 * An expense is paid by one user and can be split among multiple participants.
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    /**
     * The total amount of the expense.
     * Uses BigDecimal for monetary precision.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * The strategy used to split this expense.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitType splitType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * The user who paid for this expense.
     * JsonBackReference to prevent infinite recursion during serialization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_id", nullable = false)
    @JsonBackReference(value = "user-expenses")
    private User paidBy;

    /**
     * The group this expense belongs to.
     * Can be null for expenses between individuals (not in a group).
     * JsonBackReference to prevent infinite recursion during serialization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonBackReference(value = "group-expenses")
    private Group group;

    /**
     * The shares of this expense distributed among participants.
     * JsonManagedReference to prevent infinite recursion during serialization.
     */
    @OneToMany(mappedBy = "expense", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "expense-shares")
    @Builder.Default
    private List<ExpenseShare> shares = new ArrayList<>();
}
