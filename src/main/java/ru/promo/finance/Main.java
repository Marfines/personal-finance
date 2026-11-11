package ru.promo.finance;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import ru.promo.finance.cli.FinanceCli;
import ru.promo.finance.core.FinanceService;
import ru.promo.finance.infra.DataDirectoryLock;
import ru.promo.finance.infra.JsonStateRepository;
import ru.promo.finance.infra.Pbkdf2PasswordHasher;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    if (args.length == 1 && args[0].equals("--help")) {
      System.out.println(
          "java -jar personal-finance.jar [--data-dir <каталог>]\nВнутри приложения: help");
      return;
    }
    if (args.length != 0 && (args.length != 2 || !args[0].equals("--data-dir"))) {
      System.err.println("Использование: java -jar personal-finance.jar [--data-dir <каталог>]");
      System.exit(2);
    }
    int exitCode;
    try {
      Path directory = Path.of(args.length == 2 ? args[1] : "data").toAbsolutePath().normalize();
      try (DataDirectoryLock ignored = new DataDirectoryLock(directory)) {
        FinanceService service =
            new FinanceService(
                new JsonStateRepository(directory.resolve("finance.json")),
                new Pbkdf2PasswordHasher(),
                Clock.systemDefaultZone());
        Console console = System.console();
        BufferedReader input =
            new BufferedReader(
                console == null
                    ? new InputStreamReader(System.in, StandardCharsets.UTF_8)
                    : console.reader());
        PrintWriter output =
            console == null
                ? new PrintWriter(System.out, true, StandardCharsets.UTF_8)
                : console.writer();
        if (console == null)
          output.println(
              "Скрытый ввод пароля недоступен: в IDE пароль может отображаться. Для скрытого ввода используйте терминал.");
        FinanceCli.PasswordInput passwords =
            () -> {
              if (console == null) return input.readLine();
              char[] password = console.readPassword();
              if (password == null) return null;
              try {
                return new String(password);
              } finally {
                Arrays.fill(password, '\0');
              }
            };
        exitCode = new FinanceCli(service, input, output, passwords).run();
      }
    } catch (Exception e) {
      System.err.println("Не удалось запустить приложение: " + e.getMessage());
      exitCode = 1;
    }
    if (exitCode != 0) System.exit(exitCode);
  }
}
