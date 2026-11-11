package ru.promo.finance.core;

import static org.junit.jupiter.api.Assertions.*;
import static ru.promo.finance.TestSupport.op;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WalletTest {
  @Test
  void decimalArithmeticIsExact() {
    Wallet wallet = new Wallet();
    wallet.createCategory("Еда");
    wallet.add(op(OperationType.INCOME, "еда", "0.10", "2026-01-01"));
    wallet.add(op(OperationType.INCOME, "Еда", "0.20", "2026-01-01"));
    assertEquals(new BigDecimal("0.30"), wallet.balance());
  }

  @Test
  void categoryNamesAreCaseInsensitive() {
    Wallet wallet = new Wallet();
    wallet.createCategory(" Еда ");
    assertThrows(IllegalArgumentException.class, () -> wallet.createCategory("еДа"));
    assertEquals("Еда", wallet.category("ЕДА").name());
  }

  @Test
  void renamingPreservesBudgetAndHistory() {
    Wallet wallet = new Wallet();
    wallet.createCategory("Еда");
    wallet.setBudget("Еда", new BigDecimal("100"));
    wallet.add(op(OperationType.EXPENSE, "Еда", "20", "2026-01-01"));
    wallet.renameCategory("Еда", "Продукты");
    assertEquals("Продукты", wallet.operations().get(0).category());
    assertEquals(new BigDecimal("100.00"), wallet.category("Продукты").budget());
    assertFalse(wallet.hasCategory("Еда"));
  }

  @Test
  void conflictingRenameLeavesWalletIntact() {
    Wallet wallet = new Wallet();
    wallet.createCategory("Еда");
    wallet.createCategory("Такси");
    WalletSnapshot before = wallet.snapshot();
    assertThrows(IllegalArgumentException.class, () -> wallet.renameCategory("Еда", "Такси"));
    assertEquals(before, wallet.snapshot());
  }

  @Test
  void deletionOnlyAllowsUnusedCategories() {
    Wallet wallet = new Wallet();
    wallet.createCategory("Еда");
    wallet.createCategory("Такси");
    wallet.add(op(OperationType.EXPENSE, "Еда", "20", "2026-01-01"));
    assertThrows(IllegalArgumentException.class, () -> wallet.deleteCategory("Еда"));
    wallet.deleteCategory("Такси");
    assertEquals(1, wallet.categories().size());
  }

  @Test
  void zeroBudgetDiffersFromAbsentBudget() {
    Wallet wallet = new Wallet();
    wallet.createCategory("Еда");
    wallet.setBudget("Еда", BigDecimal.ZERO);
    assertEquals(new BigDecimal("0.00"), wallet.category("Еда").budget());
    wallet.setBudget("Еда", null);
    assertNull(wallet.category("Еда").budget());
    assertThrows(
        IllegalArgumentException.class, () -> wallet.setBudget("Еда", BigDecimal.ONE.negate()));
  }

  @Test
  void rejectsUnknownCategoryAndDuplicateOperations() {
    Wallet wallet = new Wallet();
    Operation operation = op(OperationType.EXPENSE, "Еда", "20", "2026-01-01");
    assertThrows(IllegalArgumentException.class, () -> wallet.add(operation));
    wallet.createCategory("Еда");
    wallet.add(operation);
    assertThrows(IllegalArgumentException.class, () -> wallet.add(operation));
    assertEquals(1, wallet.operations().size());
  }

  @Test
  void snapshotCollectionsAreImmutableAndIndependent() {
    Wallet wallet = new Wallet();
    wallet.createCategory("Еда");
    WalletSnapshot snapshot = wallet.snapshot();
    assertThrows(UnsupportedOperationException.class, () -> snapshot.categories().clear());
    wallet.createCategory("Такси");
    assertEquals(1, snapshot.categories().size());
  }

  @Test
  void restoreRejectsBrokenCategoryReferences() {
    WalletSnapshot invalid =
        new WalletSnapshot(
            1, List.of(), List.of(op(OperationType.EXPENSE, "Еда", "1", "2026-01-01")));
    assertThrows(IllegalArgumentException.class, () -> Wallet.restore(invalid));
  }

  @Test
  void filtersIncludeBothDatesAndDeduplicateCategories() {
    Wallet wallet = new Wallet();
    wallet.createCategory("Еда");
    wallet.createCategory("Такси");
    wallet.add(op(OperationType.EXPENSE, "Еда", "10", "2026-01-01"));
    wallet.add(op(OperationType.EXPENSE, "Такси", "20", "2026-01-31"));
    wallet.add(op(OperationType.EXPENSE, "Еда", "40", "2026-02-01"));
    var report =
        new ReportService()
            .calculate(
                wallet,
                new ReportFilter(
                    Set.of("еда", "ЕДА", "Такси"),
                    LocalDate.parse("2026-01-01"),
                    LocalDate.parse("2026-01-31")));
    assertEquals(new BigDecimal("30.00"), report.expense());
    assertEquals(new BigDecimal("-70.00"), report.balance());
    assertEquals(2, report.operations().size());
  }

  @Test
  void filtersRejectUnknownCategoryAndReversedPeriod() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ReportService()
                .calculate(new Wallet(), new ReportFilter(Set.of("нет"), null, null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ReportFilter(Set.of(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
  }

  @Test
  void moneyValidationRejectsInvalidFormatsAndRanges() {
    for (String invalid : List.of("-1", "0", "1.001", "NaN", "1e3", "", "1 000", "1000000000000")) {
      assertThrows(
          IllegalArgumentException.class, () -> Validation.parseMoney(invalid, false), invalid);
    }
    assertEquals(new BigDecimal("12.34"), Validation.parseMoney("12,34", false));
    assertThrows(
        IllegalArgumentException.class, () -> Validation.money(new BigDecimal("1.001"), false));
  }

  @Test
  void rejectsBlankAndControlCharacterNames() {
    for (String name : List.of(" ", "a\nb", "a\u001bb", "x".repeat(81)))
      assertThrows(IllegalArgumentException.class, () -> new Category(name, null));
  }
}
