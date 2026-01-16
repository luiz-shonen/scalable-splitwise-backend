package com.splitwise.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a user in the Splitwise system.
 * Contains user details and relationships to groups, expenses, and shares.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Groups that this user is a member of.
     * Uses mappedBy to indicate the owning side is in Group entity.
     */
    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Group> groups = new HashSet<>();

    /**
     * Expenses that this user has paid for.
     * JsonManagedReference to prevent infinite recursion during serialization.
     */
    @OneToMany(mappedBy = "paidBy", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "user-expenses")
    @Builder.Default
    private List<Expense> expensesPaid = new ArrayList<>();

    /**
     * Shares of expenses that this user owes.
     * JsonManagedReference to prevent infinite recursion during serialization.
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "user-shares")
    @Builder.Default
    private List<ExpenseShare> expenseShares = new ArrayList<>();

    /**
     * Groups created by this user.
     */
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "user-created-groups")
    @Builder.Default
    private List<Group> createdGroups = new ArrayList<>();
}
