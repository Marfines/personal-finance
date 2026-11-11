package ru.promo.finance.cli;

import java.math.BigDecimal;
import java.util.Locale;
import ru.promo.finance.core.Operation;
import ru.promo.finance.core.OperationType;
import ru.promo.finance.core.ReportService;

public final class ReportFormatter {
  private ReportFormatter() {}

  public static String money(BigDecimal value) {
    return value == null ? "—" : String.format(Locale.US, "%,.2f", value);
  }

  public static String summary(ReportService.Report report) {
    StringBuilder result = new StringBuilder();
    result.append("Общий доход (выборка): ").append(money(report.income())).append('\n');
    result.append("Общие расходы (выборка): ").append(money(report.expense())).append('\n');
    result.append("Текущий баланс (все операции): ").append(money(report.balance())).append('\n');
    result.append(
        "Бюджеты ниже сопоставлены с расходами выбранного периода. Лимиты не сбрасываются автоматически.\n");
    int width =
        Math.max(
            24, report.categories().stream().mapToInt(cat -> cat.name().length()).max().orElse(0));
    String format = "%-" + width + "s | %18s | %18s | %18s | %18s%n";
    result.append(String.format(format, "Категория", "Доходы", "Расходы", "Бюджет", "Остаток"));
    for (ReportService.CategoryTotal row : report.categories()) {
      result.append(
          String.format(
              format,
              row.name(),
              money(row.income()),
              money(row.expense()),
              money(row.budget()),
              money(row.remaining())));
    }
    if (report.operations().isEmpty()) result.append("Нет операций по выбранным условиям.\n");
    return result.toString();
  }

  public static String history(ReportService.Report report) {
    StringBuilder result =
        new StringBuilder("Дата       | Тип     | Сумма              | Категория | Комментарий\n");
    for (Operation op : report.operations())
      result.append(
          String.format(
              "%s | %-7s | %18s | %s | %s%n",
              op.date(),
              op.type() == OperationType.INCOME ? "Доход" : "Расход",
              money(op.amount()),
              op.category(),
              op.note()));
    if (report.operations().isEmpty()) result.append("Нет операций по выбранным условиям.\n");
    return result.toString();
  }
}
