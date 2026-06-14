package com.spreetail.expenses.repository;

import com.spreetail.expenses.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    Optional<UserAccount> findByNameIgnoreCase(String name);
    boolean existsByEmailIgnoreCase(String email);
}
