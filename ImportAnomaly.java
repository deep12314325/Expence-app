package com.spreetail.expenses.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class ImportAnomaly {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ImportBatch batch;

    private int rowNumber;

    @Enumerated(EnumType.STRING)
    private AnomalySeverity severity;

    @Enumerated(EnumType.STRING)
    private AnomalyAction action;

    private String code;
    private String message;

    protected ImportAnomaly() {
    }

    public ImportAnomaly(ImportBatch batch, int rowNumber, AnomalySeverity severity,
                         AnomalyAction action, String code, String message) {
        this.batch = batch;
        this.rowNumber = rowNumber;
        this.severity = severity;
        this.action = action;
        this.code = code;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public AnomalySeverity getSeverity() {
        return severity;
    }

    public AnomalyAction getAction() {
        return action;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
