package ru.promo.finance.core;

import static org.junit.jupiter.api.Assertions.*;
import static ru.promo.finance.TestSupport.op;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AlertServiceTest {
  private Wallet wallet(String expense) {
    Wallet wallet = new Wallet();
    wallet.createCategory("Еда");
    wallet.setBudget("Еда", new BigDecimal("100"));
    wallet.add(op(OperationType.INCOME, "Еда", "200", "2026-01-01"));
    wallet.add(op(OperationType.EXPENSE, "Еда", expense, "2026-01-01"));
    return wallet;
  }

  @Test
  void noWarningBelowThreshold() {
    assertTrue(new AlertService().alerts(wallet("79.99")).isEmpty());
  }

  @Test
  void warnsAtEightyPercent() {
    assertTrue(new AlertService().alerts(wallet("80")).get(0).contains("80%"));
  }

  @Test
  void warnsAtExactLimit() {
    assertTrue(new AlertService().alerts(wallet("100")).get(0).contains("исчерпан"));
  }

  @Test
  void warnsAboveLimitAndWhenExpensesExceedIncome() {
    var alerts = new AlertService().alerts(wallet("201"));
    assertTrue(alerts.stream().anyMatch(s -> s.contains("Превышен бюджет")));
    assertTrue(alerts.stream().anyMatch(s -> s.contains("Расходы превысили доходы")));
  }

  @Test
  void warnsAtZeroBalance() {
    assertTrue(new AlertService().alerts(new Wallet()).get(0).contains("нулю"));
  }
}
