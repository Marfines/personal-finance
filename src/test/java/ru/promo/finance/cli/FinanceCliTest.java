package ru.promo.finance.cli;

import static org.junit.jupiter.api.Assertions.*;
import static ru.promo.finance.TestSupport.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.promo.finance.core.*;

class FinanceCliTest {
  @Test
  void filtersByNamesContainingCommas() throws Exception {
    FinanceService service = loggedIn();
    Run result =
        run(
            service,
            "category add \"Еда, продукты\"\ncategory add Такси\nexpense \"Еда, продукты\" 10\nexpense Такси 20\nreport --category \"Еда, продукты\" --category Такси\nexit\n");
    assertFalse(result.output().contains("Ошибка:"));
    assertTrue(result.output().contains("Общие расходы (выборка): 30.00"));
  }

  private record Run(int code, String output) {}

  private Run run(FinanceService service, String commands) {
    BufferedReader input = new BufferedReader(new StringReader(commands));
    StringWriter output = new StringWriter();
    int code = new FinanceCli(service, input, new PrintWriter(output), input::readLine).run();
    return new Run(code, output.toString());
  }

  @Test
  void tokenizesQuotedNamesAndWindowsPaths() {
    assertEquals(
        List.of("export", "json", "C:\\Users\\user\\my file.json"),
        CommandParser.parse("export json \"C:\\Users\\user\\my file.json\""));
    assertEquals(
        List.of("category", "add", "Коммунальные услуги"),
        CommandParser.parse("category add 'Коммунальные услуги'"));
    assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("category add \"Еда"));
  }

  @Test
  void completeSessionSavesOnExit() throws Exception {
    MemoryRepository repository = new MemoryRepository();
    FinanceService service = service(repository);
    Run run =
        run(
            service,
            "register alice\n"
                + PASSWORD
                + "\nlogin alice\n"
                + PASSWORD
                + "\ncategory add Зарплата\nincome Зарплата 63000\nreport\nexit\n");
    assertEquals(0, run.code());
    assertTrue(run.output().contains("63,000.00"));
    assertEquals(1, repository.state.accounts().size());
    assertEquals(
        new BigDecimal("63000.00"),
        Wallet.restore(repository.state.accounts().get(0).wallet()).balance());
  }

  @Test
  void commandLoopSurvivesInvalidInput() throws Exception {
    FinanceService service = loggedIn();
    Run result =
        run(
            service,
            "unknown\nincome\ncategory add \"\"\ncategory add Еда\nexpense Еда -1\nexpense Еда 1 2026-02-30\nreport --categories Отсутствует\nreport --from\nreport --what yes\nexpense Еда 10\nexit\n");
    assertEquals(0, result.code());
    assertTrue(result.output().contains("Категория не найдена"));
    assertTrue(result.output().contains("ГГГГ-ММ-ДД"));
    assertEquals(1, service.snapshot().operations().size());
  }

  @Test
  void eofSavesEvenWithoutExplicitExit() throws Exception {
    MemoryRepository repository = new MemoryRepository();
    FinanceService service = service(repository);
    service.register("alice", PASSWORD);
    assertEquals(0, run(service, "help\n").code());
    assertEquals(1, repository.state.accounts().size());
  }

  @Test
  void eofReportsFailedSaveWithNonzeroExitCode() throws Exception {
    MemoryRepository repository = new MemoryRepository();
    repository.failSave = true;
    Run result = run(service(repository), "");
    assertEquals(1, result.code());
    assertTrue(result.output().contains("Не удалось сохранить"));
  }

  @Test
  void emptySelectionIsReported() throws Exception {
    Run result = run(loggedIn(), "report --from 2026-01-01 --to 2026-01-02\nexit\n");
    assertTrue(result.output().contains("Нет операций"));
  }

  @Test
  void importNeedsExplicitReplaceFlag() throws Exception {
    Run result = run(loggedIn(), "import json does-not-exist.json\nexit\n");
    assertTrue(result.output().contains("--replace"));
    assertFalse(result.output().contains("Ошибка файла"));
  }
}
