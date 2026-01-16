package com.splitwise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.splitwise.entity.User;
import com.splitwise.entity.UserBalance;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {
    Optional<UserBalance> findByFromUserAndToUser(User fromUser, User toUser);
    List<UserBalance> findAllByFromUserOrToUser(User fromUser, User toUser);
}
