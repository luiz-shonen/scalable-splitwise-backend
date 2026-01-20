package com.splitwise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.splitwise.entity.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId ORDER BY e.createdAt DESC")
    List<Expense> findByGroupId(@Param("groupId") Long groupId);
    
    @Query("SELECT DISTINCT e FROM Expense e LEFT JOIN e.shares s WHERE e.paidBy.id = :userId OR s.user.id = :userId ORDER BY e.createdAt DESC")
    List<Expense> findAllUserExpenses(@Param("userId") Long userId);
}
