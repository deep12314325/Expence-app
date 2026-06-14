package com.spreetail.expenses.repository;

import com.spreetail.expenses.domain.Expense;
import com.spreetail.expenses.domain.ExpenseGroup;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupOrderBySpentOnAscIdAsc(ExpenseGroup group);
    List<Expense> findByGroupAndSpentOn(ExpenseGroup group, LocalDate spentOn);
    boolean existsBySourceRowHash(String sourceRowHash);
}
