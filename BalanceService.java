package com.spreetail.expenses.service;

import com.spreetail.expenses.domain.Expense;
import com.spreetail.expenses.domain.ExpenseGroup;
import com.spreetail.expenses.domain.ExpenseShare;
import com.spreetail.expenses.domain.PaymentSettlement;
import com.spreetail.expenses.dto.BalanceDto;
import com.spreetail.expenses.repository.ExpenseRepository;
import com.spreetail.expenses.repository.PaymentSettlementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {
    private final GroupService groupService;
    private final ExpenseRepository expenses;
    private final PaymentSettlementRepository payments;

    public BalanceService(GroupService groupService, ExpenseRepository expenses, PaymentSettlementRepository payments) {
        this.groupService = groupService;
        this.expenses = expenses;
        this.payments = payments;
    }

    public BalanceDto balances(Long groupId) {
        ExpenseGroup group = groupService.defaultGroup();
        Map<String, BigDecimal> nets = new LinkedHashMap<>();
        List<BalanceDto.ExplanationLine> explanation = new ArrayList<>();

        for (Expense expense : expenses.findByGroupOrderBySpentOnAscIdAsc(group)) {
            credit(nets, expense.getPaidBy().getName(), expense.getAmountInInr());
            explanation.add(new BalanceDto.ExplanationLine(
                    expense.getPaidBy().getName(),
                    expense.getDescription(),
                    "Paid " + expense.getCurrency() + " " + expense.getOriginalAmount(),
                    expense.getAmountInInr()
            ));
            for (ExpenseShare share : expense.getShares()) {
                debit(nets, share.getUser().getName(), share.getAmountInInr());
                explanation.add(new BalanceDto.ExplanationLine(
                        share.getUser().getName(),
                        expense.getDescription(),
                        "Share owed",
                        share.getAmountInInr().negate()
                ));
            }
        }

        for (PaymentSettlement payment : payments.findByGroupOrderByPaidOnAscIdAsc(group)) {
            credit(nets, payment.getFromUser().getName(), payment.getAmountInInr());
            debit(nets, payment.getToUser().getName(), payment.getAmountInInr());
            explanation.add(new BalanceDto.ExplanationLine(
                    payment.getFromUser().getName(),
                    "Payment to " + payment.getToUser().getName(),
                    "Settlement recorded",
                    payment.getAmountInInr()
            ));
            explanation.add(new BalanceDto.ExplanationLine(
                    payment.getToUser().getName(),
                    "Payment from " + payment.getFromUser().getName(),
                    "Settlement received",
                    payment.getAmountInInr().negate()
            ));
        }

        List<BalanceDto.PersonBalance> balances = nets.entrySet().stream()
                .map(e -> new BalanceDto.PersonBalance(e.getKey(), money(e.getValue())))
                .sorted(Comparator.comparing(BalanceDto.PersonBalance::person))
                .toList();
        return new BalanceDto(balances, suggestSettlements(nets), explanation);
    }

    private List<BalanceDto.SettlementSuggestion> suggestSettlements(Map<String, BigDecimal> nets) {
        List<Map.Entry<String, BigDecimal>> creditors = nets.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(e -> Map.entry(e.getKey(), money(e.getValue())))
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();
        List<Map.Entry<String, BigDecimal>> debtors = nets.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(e -> Map.entry(e.getKey(), money(e.getValue().abs())))
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();

        List<BalanceDto.SettlementSuggestion> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        List<BigDecimal> owe = new ArrayList<>(debtors.stream().map(Map.Entry::getValue).toList());
        List<BigDecimal> receive = new ArrayList<>(creditors.stream().map(Map.Entry::getValue).toList());
        while (i < debtors.size() && j < creditors.size()) {
            BigDecimal amount = owe.get(i).min(receive.get(j));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                result.add(new BalanceDto.SettlementSuggestion(debtors.get(i).getKey(), creditors.get(j).getKey(), money(amount)));
            }
            owe.set(i, owe.get(i).subtract(amount));
            receive.set(j, receive.get(j).subtract(amount));
            if (owe.get(i).compareTo(BigDecimal.ZERO) == 0) {
                i++;
            }
            if (receive.get(j).compareTo(BigDecimal.ZERO) == 0) {
                j++;
            }
        }
        return result;
    }

    private void credit(Map<String, BigDecimal> nets, String person, BigDecimal amount) {
        nets.put(person, nets.getOrDefault(person, BigDecimal.ZERO).add(amount));
    }

    private void debit(Map<String, BigDecimal> nets, String person, BigDecimal amount) {
        nets.put(person, nets.getOrDefault(person, BigDecimal.ZERO).subtract(amount));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
