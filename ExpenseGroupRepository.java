package com.spreetail.expenses.repository;

import com.spreetail.expenses.domain.ExpenseGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {
    Optional<ExpenseGroup> findByNameIgnoreCase(String name);
}
