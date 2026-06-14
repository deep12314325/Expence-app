package com.spreetail.expenses.dto;

import java.time.LocalDate;

public record MemberSummary(Long id, String name, LocalDate joinedOn, LocalDate leftOn) {
}
