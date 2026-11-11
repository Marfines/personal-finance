package ru.promo.finance.core;

public interface PasswordHasher {
  record Credential(String salt, String hash) {}

  Credential hash(String password);

  boolean matches(String password, String salt, String hash);
}
