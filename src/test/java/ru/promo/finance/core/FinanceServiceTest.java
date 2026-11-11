package ru.promo.finance.core;

import static org.junit.jupiter.api.Assertions.*;
import static ru.promo.finance.TestSupport.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.promo.finance.infra.Pbkdf2PasswordHasher;

class FinanceServiceTest {
  @Test
  void reproducesSpecificationExample() throws Exception {
    FinanceService service = loggedIn();
    sample(service);
    var report = service.report(ReportFilter.all());
    assertEquals(new BigDecimal("63000.00"), report.income());
    assertEquals(new BigDecimal("8300.00"), report.expense());
    assertEquals(new BigDecimal("54700.00"), report.balance());
    assertEquals(
        new BigDecimal("60000.00"),
        report.categories().stream()
            .filter(c -> c.name().equals("Зарплата"))
            .findFirst()
            .orElseThrow()
            .income());
    assertEquals(
        List.of(new BigDecimal("3200.00"), new BigDecimal("0.00"), new BigDecimal("-500.00")),
        report.categories().stream()
            .filter(c -> c.budget() != null)
            .map(ReportService.CategoryTotal::remaining)
            .toList());
  }

  @Test
  void loginAndRegistrationValidateCredentials() throws Exception {
    FinanceService service = service(new MemoryRepository());
    assertThrows(IllegalArgumentException.class, () -> service.register("../x", PASSWORD));
    assertThrows(IllegalArgumentException.class, () -> service.register("alice", "short"));
    service.register("Alice", PASSWORD);
    assertThrows(IllegalArgumentException.class, () -> service.register("ALICE", PASSWORD));
    assertThrows(IllegalArgumentException.class, () -> service.login("alice", "incorrect"));
    assertThrows(IllegalArgumentException.class, () -> service.login("nobody", PASSWORD));
    service.login("ALICE", PASSWORD);
    assertEquals("alice", service.currentLogin());
    assertThrows(IllegalArgumentException.class, () -> service.login("alice", PASSWORD));
  }

  @Test
  void walletsAreIsolatedAcrossSessions() throws Exception {
    FinanceService service = loggedIn();
    service.createCategory("Личное");
    service.register("bob", PASSWORD);
    service.logout();
    service.login("bob", PASSWORD);
    assertTrue(service.categories().isEmpty());
    service.logout();
    service.login("alice", PASSWORD);
    assertEquals("Личное", service.categories().get(0).name());
  }

  @Test
  void guestCannotAccessWallet() throws Exception {
    FinanceService service = service(new MemoryRepository());
    assertThrows(IllegalArgumentException.class, service::snapshot);
    assertThrows(IllegalArgumentException.class, () -> service.createCategory("Еда"));
    assertThrows(IllegalArgumentException.class, () -> service.transfer("bob", BigDecimal.ONE));
  }

  @Test
  void transferCreatesLinkedOperationsAndConservesMoney() throws Exception {
    FinanceService service = loggedIn();
    service.register("bob", PASSWORD);
    service.createCategory("Зарплата");
    service.addOperation(OperationType.INCOME, "Зарплата", new BigDecimal("100"), null, "");
    service.transfer("BOB", new BigDecimal("40"));
    Operation debit = service.snapshot().operations().get(1);
    assertEquals(new BigDecimal("60.00"), service.report(ReportFilter.all()).balance());
    assertEquals("bob", debit.counterparty());
    service.logout();
    service.login("bob", PASSWORD);
    Operation credit = service.snapshot().operations().get(0);
    assertEquals(new BigDecimal("40.00"), service.report(ReportFilter.all()).balance());
    assertEquals(debit.transferId(), credit.transferId());
    assertEquals("alice", credit.counterparty());
    assertEquals(OperationType.INCOME, credit.type());
  }

  @Test
  void invalidTransfersLeaveSenderUnchanged() throws Exception {
    FinanceService service = loggedIn();
    service.register("bob", PASSWORD);
    WalletSnapshot before = service.snapshot();
    assertThrows(IllegalArgumentException.class, () -> service.transfer("alice", BigDecimal.ONE));
    assertThrows(IllegalArgumentException.class, () -> service.transfer("nobody", BigDecimal.ONE));
    assertThrows(IllegalArgumentException.class, () -> service.transfer("bob", BigDecimal.ONE));
    assertThrows(IllegalArgumentException.class, () -> service.transfer("bob", BigDecimal.ZERO));
    assertEquals(before, service.snapshot());
  }

  @Test
  void rejectsFutureOperationsWithoutMutation() throws Exception {
    FinanceService service = loggedIn();
    service.createCategory("Еда");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.addOperation(
                OperationType.EXPENSE, "Еда", BigDecimal.ONE, LocalDate.of(2026, 6, 16), ""));
    assertTrue(service.snapshot().operations().isEmpty());
  }

  @Test
  void failedLogoutKeepsSessionAndData() throws Exception {
    MemoryRepository repository = new MemoryRepository();
    FinanceService service = service(repository);
    service.register("alice", PASSWORD);
    service.login("alice", PASSWORD);
    service.createCategory("Еда");
    repository.failSave = true;
    assertThrows(java.io.IOException.class, service::logout);
    assertEquals("alice", service.currentLogin());
    assertEquals(1, service.categories().size());
    repository.failSave = false;
    service.logout();
    assertNull(service.currentLogin());
  }

  @Test
  void importsValidSnapshotAndRejectsInvalidOneAtomically() throws Exception {
    FinanceService service = loggedIn();
    WalletSnapshot valid =
        new WalletSnapshot(
            1,
            List.of(new Category("Еда", new BigDecimal("50"))),
            List.of(op(OperationType.EXPENSE, "Еда", "10", "2026-01-01")));
    service.importSnapshot(valid);
    assertEquals(valid, service.snapshot());
    assertThrows(
        IllegalArgumentException.class,
        () -> service.importSnapshot(new WalletSnapshot(1, List.of(), valid.operations())));
    assertEquals(valid, service.snapshot());
  }

  @Test
  void importCannotRemoveTransferSide() throws Exception {
    FinanceService service = loggedIn();
    service.register("bob", PASSWORD);
    service.createCategory("Доход");
    service.addOperation(OperationType.INCOME, "Доход", BigDecimal.TEN, null, "");
    service.transfer("bob", BigDecimal.ONE);
    WalletSnapshot before = service.snapshot();
    assertThrows(
        IllegalArgumentException.class, () -> service.importSnapshot(new Wallet().snapshot()));
    service.importSnapshot(before);
    assertEquals(before, service.snapshot());
  }

  @Test
  void passwordsHaveIndependentSaltAndNoPlaintext() {
    var hasher = new Pbkdf2PasswordHasher();
    var first = hasher.hash(PASSWORD);
    var second = hasher.hash(PASSWORD);
    assertNotEquals(first.salt(), second.salt());
    assertNotEquals(first.hash(), second.hash());
    assertTrue(hasher.matches(PASSWORD, first.salt(), first.hash()));
    assertFalse(hasher.matches("wrong", first.salt(), first.hash()));
    assertFalse(first.hash().contains(PASSWORD));
  }
}
