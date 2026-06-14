package com.spreetail.expenses.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseDto(
        Long id,
        LocalDate spentOn,
        String description,
        String paidBy,
        BigDecimal originalAmount,
        String currency,
        BigDecimal amountInInr,
        String splitType,
        boolean needsReview,
        boolean refund,
        List<ShareDto> shares
) {
    public record ShareDto(String user, BigDecimal amountInInr) {
    }
}
