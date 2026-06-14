package com.spreetail.expenses.repository;

import com.spreetail.expenses.domain.ExpenseGroup;
import com.spreetail.expenses.domain.GroupMembership;
import com.spreetail.expenses.domain.UserAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    List<GroupMembership> findByGroup(ExpenseGroup group);
    boolean existsByGroupAndUser(ExpenseGroup group, UserAccount user);
}
