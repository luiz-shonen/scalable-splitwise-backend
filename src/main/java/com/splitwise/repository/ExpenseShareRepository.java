package com.splitwise.repository;

import com.splitwise.entity.ExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {
    List<ExpenseShare> findByUserId(Long userId);
    List<ExpenseShare> findByUserIdAndSettledFalse(Long userId);
}
