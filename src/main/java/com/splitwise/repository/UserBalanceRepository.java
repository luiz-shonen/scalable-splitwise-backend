package com.splitwise.repository;

import com.splitwise.entity.User;
import com.splitwise.entity.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {
    Optional<UserBalance> findByFromUserAndToUser(User fromUser, User toUser);
}
