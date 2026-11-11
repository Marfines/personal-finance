package ru.promo.finance;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import ru.promo.finance.core.*;
import ru.promo.finance.infra.Pbkdf2PasswordHasher;

public final class TestSupport {
  public static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
  public static final String PASSWORD = "test-password";

  private TestSupport() {}

  public static final class MemoryRepository implements StateRepository {
    public ApplicationState state = ApplicationState.empty();
    public boolean failSave;

    @Override
    public ApplicationState load() {
      return state;
    }

    @Override
    public void save(ApplicationState value) throws IOException {
      if (failSave) throw new IOException("test: disk full");
      state = value;
    }
  }

  public static FinanceService service(StateRepository repository) throws IOException {
    return new FinanceService(repository, new Pbkdf2PasswordHasher(), CLOCK);
  }

  public static FinanceService loggedIn() throws IOException {
    FinanceService service = service(new MemoryRepository());
    service.register("alice", PASSWORD);
    service.login("alice", PASSWORD);
    return service;
  }

  public static Operation op(OperationType type, String category, String amount, String date) {
    return new Operation(
        UUID.randomUUID(),
        LocalDate.parse(date),
        type,
        category,
        new BigDecimal(amount),
        "",
        null,
        null);
  }

  public static void sample(FinanceService service) {
    for (String name :
        new String[] {"Еда", "Развлечения", "Коммунальные услуги", "Такси", "Зарплата", "Бонус"})
      service.createCategory(name);
    service.addOperation(OperationType.EXPENSE, "Еда", new BigDecimal("300"), null, "");
    service.addOperation(OperationType.EXPENSE, "Еда", new BigDecimal("500"), null, "");
    service.addOperation(OperationType.EXPENSE, "Развлечения", new BigDecimal("3000"), null, "");
    service.addOperation(
        OperationType.EXPENSE, "Коммунальные услуги", new BigDecimal("3000"), null, "");
    service.addOperation(OperationType.EXPENSE, "Такси", new BigDecimal("1500"), null, "");
    service.addOperation(OperationType.INCOME, "Зарплата", new BigDecimal("20000"), null, "");
    service.addOperation(OperationType.INCOME, "Зарплата", new BigDecimal("40000"), null, "");
    service.addOperation(OperationType.INCOME, "Бонус", new BigDecimal("3000"), null, "");
    service.setBudget("Еда", new BigDecimal("4000"));
    service.setBudget("Развлечения", new BigDecimal("3000"));
    service.setBudget("Коммунальные услуги", new BigDecimal("2500"));
  }
}
