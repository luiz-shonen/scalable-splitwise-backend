package com.splitwise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing the consolidated balance between two users.
 * This entity maintains a running balance to avoid O(N) calculations
 * when determining how much one user owes another.
 *
 * <p>The balance represents how much {@code fromUser} owes to {@code toUser}.
 * A positive value means fromUser owes toUser that amount.
 * A negative value means toUser owes fromUser (though typically we only store one direction).</p>
 *
 * <p>To ensure consistency, we always store the balance with the user having
 * the lower ID as {@code fromUser}. The sign of the balance determines the direction.</p>
 */
@Entity
@Table(
        name = "user_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_balance_pair",
                        columnNames = {"from_user_id", "to_user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_user_balance_from", columnList = "from_user_id"),
                @Index(name = "idx_user_balance_to", columnList = "to_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owes the balance (when positive).
     * Always the user with the lower ID to ensure uniqueness.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    /**
     * The user who is owed the balance (when positive).
     * Always the user with the higher ID to ensure uniqueness.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    /**
     * The consolidated balance between the two users.
     * Positive: fromUser owes toUser.
     * Negative: toUser owes fromUser.
     * Uses BigDecimal for monetary precision.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Updates the balance by adding the specified amount.
     * Positive amount increases what fromUser owes to toUser.
     *
     * @param amount the amount to add to the balance
     */
    public void addToBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if the balance is settled (zero).
     *
     * @return true if the balance is zero
     */
    public boolean isSettled() {
        return this.balance.compareTo(BigDecimal.ZERO) == 0;
    }
}
