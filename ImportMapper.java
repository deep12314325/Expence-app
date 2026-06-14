package com.spreetail.expenses.service;

import com.spreetail.expenses.domain.ImportBatch;
import com.spreetail.expenses.domain.ImportAnomaly;
import com.spreetail.expenses.dto.AnomalyDto;
import com.spreetail.expenses.dto.ImportReportDto;

public final class ImportMapper {
    private ImportMapper() {
    }

    public static ImportReportDto toDto(ImportBatch batch) {
        return new ImportReportDto(
                batch.getId(),
                batch.getFileName(),
                batch.getImportedAt(),
                batch.getTotalRows(),
                batch.getImportedExpenses(),
                batch.getImportedPayments(),
                batch.getReviewRows(),
                batch.getSkippedRows(),
                batch.getAnomalies().stream().map(ImportMapper::anomaly).toList()
        );
    }

    private static AnomalyDto anomaly(ImportAnomaly anomaly) {
        return new AnomalyDto(
                anomaly.getRowNumber(),
                anomaly.getSeverity().name(),
                anomaly.getAction().name(),
                anomaly.getCode(),
                anomaly.getMessage()
        );
    }
}
