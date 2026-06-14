package com.spreetail.expenses.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ExpenseGroup group;

    private String fileName;
    private Instant importedAt;
    private int totalRows;
    private int importedExpenses;
    private int importedPayments;
    private int reviewRows;
    private int skippedRows;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportAnomaly> anomalies = new ArrayList<>();

    protected ImportBatch() {
    }

    public ImportBatch(ExpenseGroup group, String fileName) {
        this.group = group;
        this.fileName = fileName;
        this.importedAt = Instant.now();
    }

    public void addAnomaly(int rowNumber, AnomalySeverity severity, AnomalyAction action, String code, String message) {
        anomalies.add(new ImportAnomaly(this, rowNumber, severity, action, code, message));
        if (action == AnomalyAction.NEEDS_REVIEW) {
            reviewRows++;
        }
        if (action == AnomalyAction.SKIPPED) {
            skippedRows++;
        }
    }

    public void rowRead() {
        totalRows++;
    }

    public void expenseImported() {
        importedExpenses++;
    }

    public void paymentImported() {
        importedPayments++;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getImportedExpenses() {
        return importedExpenses;
    }

    public int getImportedPayments() {
        return importedPayments;
    }

    public int getReviewRows() {
        return reviewRows;
    }

    public int getSkippedRows() {
        return skippedRows;
    }

    public List<ImportAnomaly> getAnomalies() {
        return anomalies;
    }
}
