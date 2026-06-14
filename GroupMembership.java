package com.spreetail.expenses.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class GroupMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ExpenseGroup group;

    @ManyToOne(optional = false)
    private UserAccount user;

    private LocalDate joinedOn;
    private LocalDate leftOn;

    protected GroupMembership() {
    }

    public GroupMembership(ExpenseGroup group, UserAccount user, LocalDate joinedOn, LocalDate leftOn) {
        this.group = group;
        this.user = user;
        this.joinedOn = joinedOn;
        this.leftOn = leftOn;
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public UserAccount getUser() {
        return user;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }

    public LocalDate getLeftOn() {
        return leftOn;
    }

    public boolean isActiveOn(LocalDate date) {
        boolean afterJoin = joinedOn == null || !date.isBefore(joinedOn);
        boolean beforeLeave = leftOn == null || !date.isAfter(leftOn);
        return afterJoin && beforeLeave;
    }
}
