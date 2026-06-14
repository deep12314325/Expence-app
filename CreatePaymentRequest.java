package com.spreetail.expenses.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentRequest(String from, String to, LocalDate paidOn, BigDecimal amountInInr, String notes) {
}
