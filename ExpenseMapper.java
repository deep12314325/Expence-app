package com.spreetail.expenses.service;

import com.spreetail.expenses.domain.Expense;
import com.spreetail.expenses.dto.ExpenseDto;

public final class ExpenseMapper {
    private ExpenseMapper() {
    }

    public static ExpenseDto toDto(Expense expense) {
        return new ExpenseDto(
                expense.getId(),
                expense.getSpentOn(),
                expense.getDescription(),
                expense.getPaidBy().getName(),
                expense.getOriginalAmount(),
                expense.getCurrency().name(),
                expense.getAmountInInr(),
                expense.getSplitType().name(),
                expense.isNeedsReview(),
                expense.isRefund(),
                expense.getShares().stream()
                        .map(s -> new ExpenseDto.ShareDto(s.getUser().getName(), s.getAmountInInr()))
                        .toList()
        );
    }
}
