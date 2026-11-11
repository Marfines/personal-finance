package ru.promo.finance.core;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

public record ReportFilter(Set<String> categories, LocalDate from, LocalDate to) {
  public ReportFilter {
    categories =
        categories == null
            ? Set.of()
            : categories.stream().map(Validation::key).collect(Collectors.toUnmodifiableSet());
    if (from != null && to != null && from.isAfter(to))
      throw new IllegalArgumentException("Начало периода позже окончания.");
  }

  public static ReportFilter all() {
    return new ReportFilter(Set.of(), null, null);
  }

  public boolean includes(Operation op) {
    return (categories.isEmpty() || categories.contains(Validation.key(op.category())))
        && (from == null || !op.date().isBefore(from))
        && (to == null || !op.date().isAfter(to));
  }
}
