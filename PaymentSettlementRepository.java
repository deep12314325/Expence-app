package com.spreetail.expenses.repository;

import com.spreetail.expenses.domain.ExpenseGroup;
import com.spreetail.expenses.domain.PaymentSettlement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSettlementRepository extends JpaRepository<PaymentSettlement, Long> {
    List<PaymentSettlement> findByGroupOrderByPaidOnAscIdAsc(ExpenseGroup group);
}
