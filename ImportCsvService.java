package com.spreetail.expenses.service;

import com.spreetail.expenses.domain.AnomalyAction;
import com.spreetail.expenses.domain.AnomalySeverity;
import com.spreetail.expenses.domain.CurrencyCode;
import com.spreetail.expenses.domain.Expense;
import com.spreetail.expenses.domain.ExpenseGroup;
import com.spreetail.expenses.domain.ImportBatch;
import com.spreetail.expenses.domain.PaymentSettlement;
import com.spreetail.expenses.domain.SplitType;
import com.spreetail.expenses.domain.UserAccount;
import com.spreetail.expenses.dto.CreateExpenseRequest;
import com.spreetail.expenses.dto.ImportReportDto;
import com.spreetail.expenses.repository.ExpenseRepository;
import com.spreetail.expenses.repository.ImportBatchRepository;
import com.spreetail.expenses.repository.PaymentSettlementRepository;
import com.spreetail.expenses.repository.UserAccountRepository;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportCsvService {
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter MMM_DD = DateTimeFormatter.ofPattern("MMM-dd", Locale.ENGLISH);

    private final BigDecimal usdToInr;
    private final GroupService groupService;
    private final UserAccountRepository users;
    private final ExpenseRepository expenses;
    private final PaymentSettlementRepository payments;
    private final ImportBatchRepository batches;

    public ImportCsvService(@Value("${app.fx.usd-to-inr}") BigDecimal usdToInr,
                            GroupService groupService,
                            UserAccountRepository users,
                            ExpenseRepository expenses,
                            PaymentSettlementRepository payments,
                            ImportBatchRepository batches) {
        this.usdToInr = usdToInr;
        this.groupService = groupService;
        this.users = users;
        this.expenses = expenses;
        this.payments = payments;
        this.batches = batches;
    }

    @Transactional
    public ImportReportDto importCsv(Long groupId, MultipartFile file) {
        ExpenseGroup group = groupService.defaultGroup();
        ImportBatch batch = new ImportBatch(group, file.getOriginalFilename());
        Set<String> fuzzySeen = new HashSet<>();

        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                batch.rowRead();
                importRow(group, batch, record, fuzzySeen);
            }
        } catch (Exception ex) {
            batch.addAnomaly(0, AnomalySeverity.ERROR, AnomalyAction.SKIPPED,
                    "IMPORT_FAILED", "The file could not be parsed: " + ex.getMessage());
        }
        return ImportMapper.toDto(batches.save(batch));
    }

    @Transactional
    public Expense createManualExpense(Long groupId, CreateExpenseRequest request) {
        ExpenseGroup group = groupService.defaultGroup();
        UserAccount payer = groupService.userByName(request.paidBy());
        CurrencyCode currency = CurrencyCode.valueOf(request.currency().toUpperCase(Locale.ROOT));
        SplitType splitType = SplitType.valueOf(request.splitType().toUpperCase(Locale.ROOT));
        BigDecimal inr = convert(request.amount(), currency);
        Expense expense = new Expense(group, request.spentOn(), request.description(), payer,
                request.amount(), currency, inr, splitType, null, request.notes(), false, request.amount().signum() < 0);
        Map<String, BigDecimal> details = request.splitDetails() == null ? Map.of() : request.splitDetails();
        buildShares(expense, splitType, details, List.copyOf(details.keySet()));
        return expenses.save(expense);
    }

    private void importRow(ExpenseGroup group, ImportBatch batch, CSVRecord record, Set<String> fuzzySeen) {
        int row = (int) record.getRecordNumber() + 1;
        Map<String, String> cells = normalizeRecord(record);
        LocalDate date = parseDate(cells.get("date"), batch, row);
        if (date == null) {
            return;
        }

        String description = cells.getOrDefault("description", "");
        Optional<UserAccount> payer = users.findByNameIgnoreCase(GroupService.cleanPersonName(cells.get("paid_by")));
        if (payer.isEmpty()) {
            batch.addAnomaly(row, AnomalySeverity.ERROR, AnomalyAction.SKIPPED,
                    "UNKNOWN_OR_MISSING_PAYER", "No known payer for '" + cells.get("paid_by") + "'.");
            return;
        }

        BigDecimal amount = parseAmount(cells.get("amount"), batch, row);
        if (amount == null) {
            return;
        }

        CurrencyCode currency = parseCurrency(cells.get("currency"), batch, row);
        if (currency == null) {
            return;
        }
        SplitType splitType = parseSplitType(cells.get("split_type"), description, cells.get("notes"), batch, row);
        if (looksLikePayment(description, cells.get("notes")) && splitPeople(cells.get("split_with")).size() == 1) {
            createPayment(group, batch, row, payer.get(), cells, date, convert(amount, currency));
            return;
        }
        if (splitType == null) {
            batch.addAnomaly(row, AnomalySeverity.ERROR, AnomalyAction.SKIPPED,
                    "MISSING_SPLIT_TYPE", "Expense row has no usable split type.");
            return;
        }

        if (amount.signum() == 0) {
            batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.NEEDS_REVIEW,
                    "ZERO_AMOUNT", "Zero amount kept out of balances until a user confirms it.");
            return;
        }

        boolean needsReview = false;
        if (amount.scale() > 2) {
            amount = amount.setScale(2, RoundingMode.HALF_UP);
            batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.NORMALIZED,
                    "AMOUNT_PRECISION", "Amount had more than 2 decimals and was rounded half-up.");
        }
        if (amount.signum() < 0) {
            batch.addAnomaly(row, AnomalySeverity.INFO, AnomalyAction.ACCEPTED,
                    "NEGATIVE_AMOUNT_REFUND", "Negative amount treated as a refund and included in balances.");
        }

        String hash = sha256(record.toString());
        if (expenses.existsBySourceRowHash(hash)) {
            batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.SKIPPED,
                    "EXACT_DUPLICATE", "This exact CSV row was already imported.");
            return;
        }
        String fuzzy = date + "|" + compact(description) + "|" + payer.get().getName().toLowerCase(Locale.ROOT);
        if (!fuzzySeen.add(fuzzy)) {
            needsReview = true;
            batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.NEEDS_REVIEW,
                    "POSSIBLE_DUPLICATE", "Similar date, payer, and description found. Kept for review, not deleted.");
        }

        List<String> splitWith = splitPeople(cells.get("split_with"));
        Map<String, BigDecimal> details = parseDetails(cells.get("split_details"));
        if (splitType == SplitType.PERCENTAGE) {
            BigDecimal percentTotal = details.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (percentTotal.compareTo(BigDecimal.valueOf(100)) != 0) {
                needsReview = true;
                batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.NEEDS_REVIEW,
                        "PERCENTAGE_TOTAL_NOT_100", "Percentage split totals " + percentTotal + "% instead of 100%.");
            }
        }
        if (splitType == SplitType.EQUAL && !details.isEmpty()) {
            needsReview = true;
            batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.NEEDS_REVIEW,
                    "SPLIT_DETAILS_CONFLICT", "Split type is equal but split_details contains custom values.");
        }
        for (String person : splitWith) {
            Optional<UserAccount> participant = users.findByNameIgnoreCase(GroupService.cleanPersonName(person));
            if (participant.isEmpty()) {
                needsReview = true;
                batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.NEEDS_REVIEW,
                        "UNKNOWN_SPLIT_MEMBER", person + " is not a known user; row kept for review.");
            } else if (!groupService.activeOn(group, participant.get(), date)) {
                needsReview = true;
                batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.NEEDS_REVIEW,
                        "INACTIVE_MEMBER_IN_SPLIT", person + " was not active in the group on " + date + ".");
            }
        }

        BigDecimal inr = convert(amount, currency);
        Expense expense = new Expense(group, date, description, payer.get(), amount, currency, inr,
                splitType, hash, cells.get("notes"), needsReview, amount.signum() < 0);
        buildShares(expense, splitType, details, splitWith);
        expenses.save(expense);
        batch.expenseImported();
    }

    private void createPayment(ExpenseGroup group, ImportBatch batch, int row, UserAccount payer,
                               Map<String, String> cells, LocalDate date, BigDecimal amount) {
        List<String> recipients = splitPeople(cells.get("split_with"));
        if (recipients.size() != 1) {
            batch.addAnomaly(row, AnomalySeverity.ERROR, AnomalyAction.SKIPPED,
                    "PAYMENT_RECIPIENT_UNCLEAR", "Settlement row needs exactly one recipient.");
            return;
        }
        UserAccount recipient = groupService.userByName(recipients.get(0));
        payments.save(new PaymentSettlement(group, payer, recipient, date, amount.abs(), cells.get("notes")));
        batch.addAnomaly(row, AnomalySeverity.INFO, AnomalyAction.CONVERTED_TO_PAYMENT,
                "SETTLEMENT_AS_PAYMENT", "Settlement row recorded as a payment, not an expense.");
        batch.paymentImported();
    }

    private void buildShares(Expense expense, SplitType splitType, Map<String, BigDecimal> details, List<String> splitWith) {
        List<UserAccount> participants = splitWith.stream()
                .map(GroupService::cleanPersonName)
                .map(users::findByNameIgnoreCase)
                .flatMap(Optional::stream)
                .toList();
        if (participants.isEmpty()) {
            expense.addShare(expense.getPaidBy(), expense.getAmountInInr());
            return;
        }

        if (splitType == SplitType.EQUAL) {
            BigDecimal each = expense.getAmountInInr().divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);
            participants.forEach(user -> expense.addShare(user, each));
            return;
        }

        BigDecimal totalBasis = details.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        for (UserAccount user : participants) {
            BigDecimal basis = details.getOrDefault(user.getName(), BigDecimal.ZERO);
            BigDecimal share = switch (splitType) {
                case UNEQUAL -> basis;
                case PERCENTAGE -> expense.getAmountInInr().multiply(basis).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                case SHARE -> totalBasis.signum() == 0
                        ? BigDecimal.ZERO
                        : expense.getAmountInInr().multiply(basis).divide(totalBasis, 2, RoundingMode.HALF_UP);
                case EQUAL -> BigDecimal.ZERO;
            };
            expense.addShare(user, share.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private Map<String, String> normalizeRecord(CSVRecord record) {
        Map<String, String> cells = new HashMap<>();
        record.toMap().forEach((key, value) -> cells.put(key.trim().toLowerCase(Locale.ROOT), value == null ? "" : value.trim()));
        return cells;
    }

    private LocalDate parseDate(String raw, ImportBatch batch, int row) {
        try {
            return LocalDate.parse(raw, DD_MM_YYYY);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate parsed = MonthDay.parse(raw, MMM_DD).atYear(2026);
                batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.NORMALIZED,
                        "AMBIGUOUS_DATE_FORMAT", "Date '" + raw + "' normalized as " + parsed + ".");
                return parsed;
            } catch (Exception ex) {
                batch.addAnomaly(row, AnomalySeverity.ERROR, AnomalyAction.SKIPPED,
                        "INVALID_DATE", "Date '" + raw + "' could not be parsed.");
                return null;
            }
        }
    }

    private BigDecimal parseAmount(String raw, ImportBatch batch, int row) {
        try {
            return new BigDecimal(raw.replace(",", ""));
        } catch (Exception ex) {
            batch.addAnomaly(row, AnomalySeverity.ERROR, AnomalyAction.SKIPPED,
                    "INVALID_AMOUNT", "Amount '" + raw + "' could not be parsed.");
            return null;
        }
    }

    private CurrencyCode parseCurrency(String raw, ImportBatch batch, int row) {
        if (raw == null || raw.isBlank()) {
            batch.addAnomaly(row, AnomalySeverity.WARNING, AnomalyAction.DEFAULTED,
                    "MISSING_CURRENCY", "Currency missing; defaulted to INR.");
            return CurrencyCode.INR;
        }
        try {
            return CurrencyCode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            batch.addAnomaly(row, AnomalySeverity.ERROR, AnomalyAction.SKIPPED,
                    "UNKNOWN_CURRENCY", "Currency '" + raw + "' is not supported.");
            return null;
        }
    }

    private SplitType parseSplitType(String raw, String description, String notes, ImportBatch batch, int row) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SplitType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            if (looksLikePayment(description, notes)) {
                return null;
            }
            batch.addAnomaly(row, AnomalySeverity.ERROR, AnomalyAction.SKIPPED,
                    "UNKNOWN_SPLIT_TYPE", "Split type '" + raw + "' is not supported.");
            return null;
        }
    }

    private Map<String, BigDecimal> parseDetails(String raw) {
        Map<String, BigDecimal> details = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return details;
        }
        for (String part : raw.split(";")) {
            String[] tokens = part.trim().replace("%", "").split("\\s+");
            if (tokens.length >= 2) {
                details.put(GroupService.cleanPersonName(tokens[0]), new BigDecimal(tokens[1].replace(",", "")));
            }
        }
        return details;
    }

    private List<String> splitPeople(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> people = new ArrayList<>();
        for (String part : raw.split(";")) {
            if (!part.isBlank()) {
                people.add(GroupService.cleanPersonName(part));
            }
        }
        return people;
    }

    private BigDecimal convert(BigDecimal amount, CurrencyCode currency) {
        BigDecimal converted = currency == CurrencyCode.USD ? amount.multiply(usdToInr) : amount;
        return converted.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean looksLikePayment(String description, String notes) {
        String text = ((description == null ? "" : description) + " " + (notes == null ? "" : notes)).toLowerCase(Locale.ROOT);
        return text.contains("paid") || text.contains("settlement") || text.contains("deposit");
    }

    private String compact(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
