package ru.promo.finance.infra;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import ru.promo.finance.core.PasswordHasher;

public final class Pbkdf2PasswordHasher implements PasswordHasher {
  private static final int ITERATIONS = 210_000;
  private final SecureRandom random = new SecureRandom();

  @Override
  public Credential hash(String password) {
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return new Credential(
        Base64.getEncoder().encodeToString(salt),
        Base64.getEncoder().encodeToString(derive(password, salt)));
  }

  @Override
  public boolean matches(String password, String salt, String hash) {
    return MessageDigest.isEqual(
        derive(password, Base64.getDecoder().decode(salt)), Base64.getDecoder().decode(hash));
  }

  private byte[] derive(String password, byte[] salt) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("PBKDF2 недоступен в установленной Java.", e);
    } finally {
      spec.clearPassword();
    }
  }
}
