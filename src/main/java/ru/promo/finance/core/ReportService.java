package ru.promo.finance.core;

import java.math.BigDecimal;
import java.util.List;

public final class ReportService {
  public record CategoryTotal(
      String name,
      BigDecimal income,
      BigDecimal expense,
      BigDecimal budget,
      BigDecimal remaining) {}

  public record Report(
      BigDecimal income,
      BigDecimal expense,
      BigDecimal balance,
      List<CategoryTotal> categories,
      List<Operation> operations) {}

  public Report calculate(Wallet wallet, ReportFilter filter) {
    filter.categories().forEach(wallet::category);
    List<Operation> selected = wallet.operations().stream().filter(filter::includes).toList();
    List<CategoryTotal> totals =
        wallet.categories().stream()
            .filter(
                cat ->
                    filter.categories().isEmpty()
                        || filter.categories().contains(Validation.key(cat.name())))
            .map(
                cat -> {
                  List<Operation> group =
                      selected.stream()
                          .filter(
                              op ->
                                  Validation.key(op.category()).equals(Validation.key(cat.name())))
                          .toList();
                  BigDecimal expense = sum(group, OperationType.EXPENSE);
                  return new CategoryTotal(
                      cat.name(),
                      sum(group, OperationType.INCOME),
                      expense,
                      cat.budget(),
                      cat.budget() == null ? null : cat.budget().subtract(expense));
                })
            .toList();
    return new Report(
        sum(selected, OperationType.INCOME),
        sum(selected, OperationType.EXPENSE),
        wallet.balance(),
        totals,
        selected);
  }

  private BigDecimal sum(List<Operation> operations, OperationType type) {
    return operations.stream()
        .filter(op -> op.type() == type)
        .map(Operation::amount)
        .reduce(new BigDecimal("0.00"), BigDecimal::add);
  }
}
