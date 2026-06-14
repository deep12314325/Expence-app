package com.spreetail.expenses.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class PaymentSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ExpenseGroup group;

    @ManyToOne(optional = false)
    private UserAccount fromUser;

    @ManyToOne(optional = false)
    private UserAccount toUser;

    private LocalDate paidOn;
    private BigDecimal amountInInr;
    private String notes;

    protected PaymentSettlement() {
    }

    public PaymentSettlement(ExpenseGroup group, UserAccount fromUser, UserAccount toUser,
                             LocalDate paidOn, BigDecimal amountInInr, String notes) {
        this.group = group;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.paidOn = paidOn;
        this.amountInInr = amountInInr;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getFromUser() {
        return fromUser;
    }

    public UserAccount getToUser() {
        return toUser;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public BigDecimal getAmountInInr() {
        return amountInInr;
    }

    public String getNotes() {
        return notes;
    }
}
