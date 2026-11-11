package ru.promo.finance.core;

import java.math.BigDecimal;

/** A null budget means no limit; zero is a real limit. */
public record Category(String name, BigDecimal budget) {
  public Category {
    name = Validation.text(name, "Категория", 80);
    if (budget != null) budget = Validation.money(budget, true);
  }
}
