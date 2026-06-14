package com.spreetail.expenses.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ExpenseGroup group;

    private LocalDate spentOn;
    private String description;

    @ManyToOne(optional = false)
    private UserAccount paidBy;

    private BigDecimal originalAmount;

    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    private BigDecimal amountInInr;

    @Enumerated(EnumType.STRING)
    private SplitType splitType;

    private String sourceRowHash;
    private String notes;
    private boolean needsReview;
    private boolean refund;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseShare> shares = new ArrayList<>();

    protected Expense() {
    }

    public Expense(ExpenseGroup group, LocalDate spentOn, String description, UserAccount paidBy,
                   BigDecimal originalAmount, CurrencyCode currency, BigDecimal amountInInr,
                   SplitType splitType, String sourceRowHash, String notes, boolean needsReview, boolean refund) {
        this.group = group;
        this.spentOn = spentOn;
        this.description = description;
        this.paidBy = paidBy;
        this.originalAmount = originalAmount;
        this.currency = currency;
        this.amountInInr = amountInInr;
        this.splitType = splitType;
        this.sourceRowHash = sourceRowHash;
        this.notes = notes;
        this.needsReview = needsReview;
        this.refund = refund;
    }

    public void addShare(UserAccount user, BigDecimal amountInInr) {
        shares.add(new ExpenseShare(this, user, amountInInr));
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSpentOn() {
        return spentOn;
    }

    public String getDescription() {
        return description;
    }

    public UserAccount getPaidBy() {
        return paidBy;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public BigDecimal getAmountInInr() {
        return amountInInr;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public boolean isRefund() {
        return refund;
    }

    public List<ExpenseShare> getShares() {
        return shares;
    }
}
