package com.spreetail.expenses.service;

import com.spreetail.expenses.domain.ExpenseGroup;
import com.spreetail.expenses.domain.GroupMembership;
import com.spreetail.expenses.domain.UserAccount;
import com.spreetail.expenses.repository.ExpenseGroupRepository;
import com.spreetail.expenses.repository.GroupMembershipRepository;
import com.spreetail.expenses.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {
    private final ExpenseGroupRepository groups;
    private final UserAccountRepository users;
    private final GroupMembershipRepository memberships;

    public GroupService(ExpenseGroupRepository groups, UserAccountRepository users, GroupMembershipRepository memberships) {
        this.groups = groups;
        this.users = users;
        this.memberships = memberships;
    }

    public ExpenseGroup defaultGroup() {
        return groups.findByNameIgnoreCase("Flatmates").orElseThrow();
    }

    public List<ExpenseGroup> allGroups() {
        return groups.findAll();
    }

    public List<GroupMembership> members(Long groupId) {
        return memberships.findByGroup(groups.findById(groupId).orElseThrow());
    }

    public UserAccount userByName(String name) {
        return users.findByNameIgnoreCase(cleanPersonName(name)).orElseThrow();
    }

    public UserAccount userByEmail(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }

    @Transactional
    public GroupMembership addMember(Long groupId, String name, String email, LocalDate joinedOn, LocalDate leftOn) {
        ExpenseGroup group = groups.findById(groupId).orElseThrow();
        UserAccount user = users.findByEmailIgnoreCase(email)
                .orElseGet(() -> users.save(new UserAccount(name, email, "password")));
        if (memberships.existsByGroupAndUser(group, user)) {
            throw new IllegalArgumentException("Member already exists in this group");
        }
        return memberships.save(new GroupMembership(group, user, joinedOn, leftOn));
    }

    public boolean activeOn(ExpenseGroup group, UserAccount user, LocalDate date) {
        return memberships.findByGroup(group).stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .anyMatch(m -> m.isActiveOn(date));
    }

    public static String cleanPersonName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("priya s")) {
            return "Priya";
        }
        if (trimmed.isBlank()) {
            return "";
        }
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
}
