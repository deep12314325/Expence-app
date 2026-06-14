package com.spreetail.expenses.service;

import com.spreetail.expenses.domain.ExpenseGroup;
import com.spreetail.expenses.domain.GroupMembership;
import com.spreetail.expenses.domain.UserAccount;
import com.spreetail.expenses.repository.ExpenseGroupRepository;
import com.spreetail.expenses.repository.GroupMembershipRepository;
import com.spreetail.expenses.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapData implements CommandLineRunner {
    private final UserAccountRepository users;
    private final ExpenseGroupRepository groups;
    private final GroupMembershipRepository memberships;

    public BootstrapData(UserAccountRepository users, ExpenseGroupRepository groups, GroupMembershipRepository memberships) {
        this.users = users;
        this.groups = groups;
        this.memberships = memberships;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ExpenseGroup group = groups.findByNameIgnoreCase("Flatmates")
                .orElseGet(() -> groups.save(new ExpenseGroup("Flatmates")));

        Map<String, String> people = Map.of(
                "Aisha", "aisha@example.com",
                "Rohan", "rohan@example.com",
                "Priya", "priya@example.com",
                "Meera", "meera@example.com",
                "Dev", "dev@example.com",
                "Sam", "sam@example.com"
        );

        people.forEach((name, email) -> users.findByEmailIgnoreCase(email)
                .orElseGet(() -> users.save(new UserAccount(name, email, "password"))));

        addMembership(group, "Aisha", LocalDate.of(2026, 2, 1), null);
        addMembership(group, "Rohan", LocalDate.of(2026, 2, 1), null);
        addMembership(group, "Priya", LocalDate.of(2026, 2, 1), null);
        addMembership(group, "Meera", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31));
        addMembership(group, "Dev", LocalDate.of(2026, 2, 8), LocalDate.of(2026, 3, 14));
        addMembership(group, "Sam", LocalDate.of(2026, 4, 8), null);
    }

    private void addMembership(ExpenseGroup group, String name, LocalDate joined, LocalDate left) {
        UserAccount user = users.findByNameIgnoreCase(name).orElseThrow();
        if (!memberships.existsByGroupAndUser(group, user)) {
            memberships.save(new GroupMembership(group, user, joined, left));
        }
    }
}
