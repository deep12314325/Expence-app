package com.spreetail.expenses.dto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceDto(
        List<PersonBalance> balances,
        List<SettlementSuggestion> settlements,
        List<ExplanationLine> explanation
) {
    public record PersonBalance(String person, BigDecimal netInInr) {
    }

    public record SettlementSuggestion(String from, String to, BigDecimal amountInInr) {
    }

    public record ExplanationLine(String person, String source, String detail, BigDecimal deltaInInr) {
    }
}
