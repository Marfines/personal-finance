package ru.promo.finance.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AlertService {
  public List<String> alerts(Wallet wallet) {
    List<String> messages = new ArrayList<>();
    for (ReportService.CategoryTotal category :
        new ReportService().calculate(wallet, ReportFilter.all()).categories()) {
      if (category.budget() == null) continue;
      if (category.remaining().signum() < 0)
        messages.add(
            "Превышен бюджет «" + category.name() + "» на " + category.remaining().negate() + ".");
      else if (category.remaining().signum() == 0)
        messages.add("Бюджет «" + category.name() + "» исчерпан.");
      else if (category.expense().compareTo(category.budget().multiply(new BigDecimal("0.80")))
          >= 0) messages.add("Использовано не менее 80% бюджета «" + category.name() + "».");
    }
    if (wallet.balance().signum() < 0)
      messages.add("Расходы превысили доходы на " + wallet.balance().negate() + ".");
    else if (wallet.balance().signum() == 0) messages.add("Баланс кошелька равен нулю.");
    return List.copyOf(messages);
  }
}
