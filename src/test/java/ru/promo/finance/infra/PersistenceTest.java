package ru.promo.finance.infra;

import static org.junit.jupiter.api.Assertions.*;
import static ru.promo.finance.TestSupport.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.promo.finance.core.*;

class PersistenceTest {
  @TempDir Path directory;

  @Test
  void rejectsMissingTransferSideOnLoad() throws Exception {
    Path file = directory.resolve("finance.json");
    FinanceService service = service(new JsonStateRepository(file));
    service.register("alice", PASSWORD);
    service.register("bob", PASSWORD);
    service.login("alice", PASSWORD);
    service.createCategory("Доход");
    service.addOperation(OperationType.INCOME, "Доход", BigDecimal.TEN, null, "");
    service.transfer("bob", BigDecimal.ONE);
    service.save();
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var tree = mapper.readTree(Files.readString(file));
    ((com.fasterxml.jackson.databind.node.ArrayNode) tree.get("accounts")).remove(1);
    Files.writeString(file, mapper.writeValueAsString(tree));
    assertThrows(java.io.IOException.class, () -> new JsonStateRepository(file).load());
  }

  @Test
  void rejectsMismatchedTransferAmountOnLoad() throws Exception {
    Path file = directory.resolve("finance.json");
    FinanceService service = service(new JsonStateRepository(file));
    service.register("alice", PASSWORD);
    service.register("bob", PASSWORD);
    service.login("alice", PASSWORD);
    service.createCategory("Доход");
    service.addOperation(OperationType.INCOME, "Доход", BigDecimal.TEN, null, "");
    service.transfer("bob", BigDecimal.ONE);
    service.save();
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var tree = mapper.readTree(Files.readString(file));
    ((com.fasterxml.jackson.databind.node.ObjectNode)
            tree.get("accounts").get(1).get("wallet").get("operations").get(0))
        .put("amount", 2);
    Files.writeString(file, mapper.writeValueAsString(tree));
    assertThrows(java.io.IOException.class, () -> new JsonStateRepository(file).load());
  }

  @Test
  void savesAndReloadsAllUsersWalletsAndTransfers() throws Exception {
    Path file = directory.resolve("finance.json");
    FinanceService original = service(new JsonStateRepository(file));
    original.register("alice", PASSWORD);
    original.register("bob", PASSWORD);
    original.login("alice", PASSWORD);
    sample(original);
    original.transfer("bob", new BigDecimal("100"));
    WalletSnapshot alice = original.snapshot();
    original.save();
    assertFalse(Files.readString(file).contains(PASSWORD));
    FinanceService restored = service(new JsonStateRepository(file));
    restored.login("alice", PASSWORD);
    assertEquals(alice, restored.snapshot());
    restored.logout();
    restored.login("bob", PASSWORD);
    assertEquals(new BigDecimal("100.00"), restored.report(ReportFilter.all()).balance());
    restored.save();
    assertEquals(2, new JsonStateRepository(file).load().accounts().size());
  }

  @Test
  void damagedFileIsNeverSilentlyReset() throws Exception {
    Path file = directory.resolve("finance.json");
    Files.writeString(file, "{broken");
    assertThrows(java.io.IOException.class, () -> service(new JsonStateRepository(file)));
    assertEquals("{broken", Files.readString(file));
  }

  @Test
  void unknownVersionAndDuplicateKeysAreRejected() throws Exception {
    Path file = directory.resolve("finance.json");
    for (String content :
        new String[] {
          "{\"version\":2,\"accounts\":[]}",
          "{\"version\":1,\"version\":1,\"accounts\":[]}",
          "null",
          "{\"version\":1,\"accounts\":[]} {}"
        }) {
      Files.writeString(file, content);
      assertThrows(java.io.IOException.class, () -> new JsonStateRepository(file).load());
    }
  }

  @Test
  void missingFileStartsEmpty() throws Exception {
    assertTrue(
        new JsonStateRepository(directory.resolve("missing.json")).load().accounts().isEmpty());
  }

  @Test
  void jsonExportCanBeImportedWithoutCredentialsAndDoesNotOverwrite() throws Exception {
    FinanceService service = loggedIn();
    sample(service);
    JsonFiles json = new JsonFiles();
    Path file = directory.resolve("wallet.json");
    json.export(file, service.snapshot());
    assertEquals(service.snapshot(), json.read(file, WalletSnapshot.class));
    assertFalse(Files.readString(file).contains("passwordHash"));
    assertThrows(
        java.nio.file.FileAlreadyExistsException.class,
        () -> json.export(file, new Wallet().snapshot()));
  }

  @Test
  void processLockPreventsConcurrentWriterAndReleases() throws Exception {
    try (DataDirectoryLock first = new DataDirectoryLock(directory)) {
      assertThrows(java.io.IOException.class, () -> new DataDirectoryLock(directory));
    }
    try (DataDirectoryLock next = new DataDirectoryLock(directory)) {
      assertNotNull(next);
    }
  }

  @Test
  void csvQuotesAndNeutralizesFormulaCells() throws Exception {
    FinanceService service = loggedIn();
    service.createCategory("=SUM(1,2)");
    service.addOperation(
        OperationType.INCOME, "=SUM(1,2)", new BigDecimal("10.25"), null, "a,\"b\"");
    Path file = directory.resolve("report.csv");
    new CsvExporter().export(file, service.report(ReportFilter.all()));
    String csv = Files.readString(file);
    assertTrue(csv.contains("\"'=SUM(1,2)\""));
    assertTrue(csv.contains("\"a,\"\"b\"\"\""));
    assertTrue(csv.contains(",10.25,"));
  }
}
