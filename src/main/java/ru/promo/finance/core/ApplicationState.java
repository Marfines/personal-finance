package ru.promo.finance.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ApplicationState(int version, List<AccountData> accounts) {
  public ApplicationState {
    if (version != 1 || accounts == null)
      throw new IllegalArgumentException("Неверный формат или версия данных приложения.");
    accounts = List.copyOf(accounts);
    Set<String> logins = new HashSet<>();
    for (AccountData account : accounts) {
      if (!logins.add(account.login()))
        throw new IllegalArgumentException("Повторяющийся логин в файле.");
    }
    validateTransfers(accounts);
  }

  private record OwnedOperation(String owner, Operation operation) {}

  private static void validateTransfers(List<AccountData> accounts) {
    Map<UUID, List<OwnedOperation>> transfers = new HashMap<>();
    for (AccountData account : accounts) {
      for (Operation operation : account.wallet().operations()) {
        if (operation.transferId() != null)
          transfers
              .computeIfAbsent(operation.transferId(), key -> new ArrayList<>())
              .add(new OwnedOperation(account.login(), operation));
      }
    }
    for (List<OwnedOperation> pair : transfers.values()) {
      if (pair.size() != 2)
        throw new IllegalArgumentException("Нарушена парность операций перевода.");
      OwnedOperation first = pair.get(0);
      OwnedOperation second = pair.get(1);
      Operation a = first.operation();
      Operation b = second.operation();
      if (first.owner().equals(second.owner())
          || a.type() == b.type()
          || a.amount().compareTo(b.amount()) != 0
          || !a.date().equals(b.date())
          || !first.owner().equals(b.counterparty())
          || !second.owner().equals(a.counterparty())) {
        throw new IllegalArgumentException("Несогласованные операции перевода.");
      }
    }
  }

  public static ApplicationState empty() {
    return new ApplicationState(1, List.of());
  }
}
