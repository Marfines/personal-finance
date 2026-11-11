package ru.promo.finance.core;

import java.util.List;

public record WalletSnapshot(int version, List<Category> categories, List<Operation> operations) {
  public WalletSnapshot {
    if (version != 1 || categories == null || operations == null)
      throw new IllegalArgumentException("Неверный формат или версия кошелька.");
    categories = List.copyOf(categories);
    operations = List.copyOf(operations);
  }
}
