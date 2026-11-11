package ru.promo.finance.core;

import java.util.Base64;

public record AccountData(String login, String salt, String passwordHash, WalletSnapshot wallet) {
  public AccountData {
    login = Validation.login(login);
    try {
      if (salt == null
          || passwordHash == null
          || Base64.getDecoder().decode(salt).length != 16
          || Base64.getDecoder().decode(passwordHash).length != 32)
        throw new IllegalArgumentException();
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Неверный формат учётной записи.");
    }
    Wallet.restore(wallet);
  }
}
