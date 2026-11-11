package ru.promo.finance.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Shared domain rules, independent of CLI and persistence. */
public final class Validation {
  private Validation() {}

  public static String text(String value, String field, int max) {
    if (value == null
        || value.isBlank()
        || value.length() > max
        || value.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException(
          field + ": требуется непустой текст до " + max + " символов без управляющих знаков.");
    }
    return value.strip();
  }

  public static String login(String value) {
    if (value == null || !value.matches("[A-Za-z0-9_-]{3,32}")) {
      throw new IllegalArgumentException("Логин: 3–32 латинских буквы, цифры, '_' или '-'.");
    }
    return value.toLowerCase(Locale.ROOT);
  }

  public static String key(String value) {
    return text(value, "Категория", 80).toLowerCase(Locale.ROOT);
  }

  public static BigDecimal money(BigDecimal value, boolean zeroAllowed) {
    if (value == null
        || value.signum() < 0
        || (!zeroAllowed && value.signum() == 0)
        || value.compareTo(new BigDecimal("999999999999.99")) > 0) {
      throw new IllegalArgumentException(
          "Сумма должна быть "
              + (zeroAllowed ? "неотрицательной" : "положительной")
              + " и не больше 999999999999.99.");
    }
    try {
      return value.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Допустимо не более двух знаков после запятой.");
    }
  }

  public static BigDecimal parseMoney(String value, boolean zeroAllowed) {
    if (value == null || !value.matches("[0-9]+([.,][0-9]{1,2})?")) {
      throw new IllegalArgumentException("Введите сумму числом, например 1500.50 или 1500,50.");
    }
    return money(new BigDecimal(value.replace(',', '.')), zeroAllowed);
  }
}
