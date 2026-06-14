package com.spreetail.expenses.dto;

import java.time.Instant;
import java.util.List;

public record ImportReportDto(
        Long id,
        String fileName,
        Instant importedAt,
        int totalRows,
        int importedExpenses,
        int importedPayments,
        int reviewRows,
        int skippedRows,
        List<AnomalyDto> anomalies
) {
}
