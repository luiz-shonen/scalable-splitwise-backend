package com.splitwise.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a group in the Splitwise system.
 * Groups contain members and expenses shared among them.
 */
@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Group extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * The user who created this group.
     * JsonBackReference to prevent infinite recursion during serialization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @JsonBackReference(value = "user-created-groups")
    private User createdBy;

    /**
     * Members of this group.
     * This is the owning side of the many-to-many relationship with User.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnore
    @Builder.Default
    private Set<User> members = new HashSet<>();

    /**
     * Expenses in this group.
     * JsonManagedReference to prevent infinite recursion during serialization.
     */
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "group-expenses")
    @Builder.Default
    private List<Expense> expenses = new ArrayList<>();


    /**
     * Adds a member to the group.
     *
     * @param user the user to add
     */
    public void addMember(User user) {
        this.members.add(user);
        user.getGroups().add(this);
    }

    /**
     * Removes a member from the group.
     *
     * @param user the user to remove
     */
    public void removeMember(User user) {
        this.members.remove(user);
        user.getGroups().remove(this);
    }
}
