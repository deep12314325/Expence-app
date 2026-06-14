package com.spreetail.expenses.dto;

public record AnomalyDto(int rowNumber, String severity, String action, String code, String message) {
}
