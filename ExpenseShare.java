package com.spreetail.expenses.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
public class ExpenseShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Expense expense;

    @ManyToOne(optional = false)
    private UserAccount user;

    private BigDecimal amountInInr;

    protected ExpenseShare() {
    }

    public ExpenseShare(Expense expense, UserAccount user, BigDecimal amountInInr) {
        this.expense = expense;
        this.user = user;
        this.amountInInr = amountInInr;
    }

    public UserAccount getUser() {
        return user;
    }

    public BigDecimal getAmountInInr() {
        return amountInInr;
    }

    public Expense getExpense() {
        return expense;
    }
}
