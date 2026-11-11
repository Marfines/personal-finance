package ru.promo.finance.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Operation(
    UUID id,
    LocalDate date,
    OperationType type,
    String category,
    BigDecimal amount,
    String note,
    String counterparty,
    UUID transferId) {
  public Operation {
    if (id == null || date == null || type == null)
      throw new IllegalArgumentException("Операция: обязательные поля отсутствуют.");
    category = Validation.text(category, "Категория", 80);
    amount = Validation.money(amount, false);
    note = note == null ? "" : note.strip();
    if (note.length() > 500 || note.chars().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException("Комментарий: до 500 символов без управляющих знаков.");
    if ((counterparty == null) != (transferId == null))
      throw new IllegalArgumentException("Неполные данные перевода.");
    if (counterparty != null) counterparty = Validation.login(counterparty);
  }

  public Operation withCategory(String name) {
    return new Operation(id, date, type, name, amount, note, counterparty, transferId);
  }
}
