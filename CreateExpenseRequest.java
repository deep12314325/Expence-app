package com.spreetail.expenses.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CreateExpenseRequest(
        LocalDate spentOn,
        String description,
        String paidBy,
        BigDecimal amount,
        String currency,
        String splitType,
        Map<String, BigDecimal> splitDetails,
        String notes
) {
}
