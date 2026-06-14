package com.spreetail.expenses.controller;

import com.spreetail.expenses.domain.PaymentSettlement;
import com.spreetail.expenses.dto.BalanceDto;
import com.spreetail.expenses.dto.CreateExpenseRequest;
import com.spreetail.expenses.dto.CreatePaymentRequest;
import com.spreetail.expenses.dto.ExpenseDto;
import com.spreetail.expenses.dto.ImportReportDto;
import com.spreetail.expenses.repository.ExpenseRepository;
import com.spreetail.expenses.repository.ImportBatchRepository;
import com.spreetail.expenses.repository.PaymentSettlementRepository;
import com.spreetail.expenses.service.BalanceService;
import com.spreetail.expenses.service.ExpenseMapper;
import com.spreetail.expenses.service.GroupService;
import com.spreetail.expenses.service.ImportCsvService;
import com.spreetail.expenses.service.ImportMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class ExpenseController {
    private final GroupService groupService;
    private final ImportCsvService importCsvService;
    private final ExpenseRepository expenses;
    private final PaymentSettlementRepository payments;
    private final ImportBatchRepository batches;
    private final BalanceService balanceService;

    public ExpenseController(GroupService groupService, ImportCsvService importCsvService,
                             ExpenseRepository expenses, PaymentSettlementRepository payments,
                             ImportBatchRepository batches, BalanceService balanceService) {
        this.groupService = groupService;
        this.importCsvService = importCsvService;
        this.expenses = expenses;
        this.payments = payments;
        this.batches = batches;
        this.balanceService = balanceService;
    }

    @GetMapping("/expenses")
    public List<ExpenseDto> listExpenses() {
        return expenses.findByGroupOrderBySpentOnAscIdAsc(groupService.defaultGroup())
                .stream()
                .map(ExpenseMapper::toDto)
                .toList();
    }

    @PostMapping("/expenses")
    public ExpenseDto createExpense(@PathVariable Long groupId, @RequestBody CreateExpenseRequest request) {
        return ExpenseMapper.toDto(importCsvService.createManualExpense(groupId, request));
    }

    @PostMapping("/payments")
    public Long createPayment(@RequestBody CreatePaymentRequest request) {
        var payment = payments.save(new PaymentSettlement(
                groupService.defaultGroup(),
                groupService.userByName(request.from()),
                groupService.userByName(request.to()),
                request.paidOn(),
                request.amountInInr(),
                request.notes()
        ));
        return payment.getId();
    }

    @GetMapping("/balances")
    public BalanceDto balances(@PathVariable Long groupId) {
        return balanceService.balances(groupId);
    }

    @PostMapping("/imports")
    public ImportReportDto importCsv(@PathVariable Long groupId, @RequestPart("file") MultipartFile file) {
        return importCsvService.importCsv(groupId, file);
    }

    @GetMapping("/imports")
    public List<ImportReportDto> reports() {
        return batches.findAllByOrderByImportedAtDesc().stream()
                .map(ImportMapper::toDto)
                .toList();
    }
}
