package ru.promo.finance.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.promo.finance.core.Category;
import ru.promo.finance.core.FinanceService;
import ru.promo.finance.core.OperationType;
import ru.promo.finance.core.ReportFilter;
import ru.promo.finance.core.Validation;
import ru.promo.finance.core.WalletSnapshot;
import ru.promo.finance.infra.CsvExporter;
import ru.promo.finance.infra.JsonFiles;

public final class FinanceCli {
  @FunctionalInterface
  public interface PasswordInput {
    String read() throws IOException;
  }

  private final FinanceService service;
  private final BufferedReader input;
  private final PrintWriter output;
  private final PasswordInput passwordInput;
  private final JsonFiles json = new JsonFiles();

  public FinanceCli(
      FinanceService service,
      BufferedReader input,
      PrintWriter output,
      PasswordInput passwordInput) {
    this.service = service;
    this.input = input;
    this.output = output;
    this.passwordInput = passwordInput;
  }

  public int run() {
    output.println("Личные финансы. Команда help — справка. Все суммы в рублях.");
    while (true) {
      output.print((service.currentLogin() == null ? "guest" : service.currentLogin()) + "> ");
      output.flush();
      String line;
      try {
        line = input.readLine();
      } catch (IOException e) {
        output.println("Ошибка чтения ввода.");
        return saveAtEnd();
      }
      if (line == null) return saveAtEnd();
      try {
        List<String> args = CommandParser.parse(line);
        if (!args.isEmpty() && execute(args)) return 0;
      } catch (DateTimeParseException e) {
        output.println("Ошибка: дата должна существовать и иметь формат ГГГГ-ММ-ДД.");
      } catch (IllegalArgumentException e) {
        output.println("Ошибка: " + e.getMessage());
      } catch (IOException e) {
        output.println(
            "Ошибка файла: "
                + e.getMessage()
                + ". Изменения остаются в памяти; проверьте путь и права.");
      }
      output.flush();
    }
  }

  private int saveAtEnd() {
    try {
      service.save();
      output.println("Данные сохранены. До свидания!");
      return 0;
    } catch (IOException e) {
      output.println("Не удалось сохранить данные: " + e.getMessage());
      return 1;
    } finally {
      output.flush();
    }
  }

  private boolean execute(List<String> a) throws IOException {
    switch (a.get(0)) {
      case "help" -> {
        count(a, 1, "help");
        output.println(HELP);
      }
      case "register", "login" -> {
        count(a, 2, a.get(0) + " <логин>");
        output.print("Пароль: ");
        output.flush();
        String password = passwordInput.read();
        if (password == null) throw new IllegalArgumentException("Ввод пароля прерван.");
        if (a.get(0).equals("register")) {
          service.register(a.get(1), password);
          output.println("Пользователь создан. Теперь выполните login " + a.get(1));
        } else {
          service.login(a.get(1), password);
          output.println("Вход выполнен: " + service.currentLogin());
          showAlerts();
        }
      }
      case "logout" -> {
        count(a, 1, "logout");
        service.logout();
        output.println("Данные сохранены. Вы вышли из аккаунта.");
      }
      case "category" -> category(a);
      case "categories" -> {
        count(a, 1, "categories");
        List<Category> categories = service.categories();
        if (categories.isEmpty())
          output.println("Категорий нет. Создайте: category add <название>");
        for (Category category : categories)
          output.println(
              category.name() + " | Бюджет: " + ReportFormatter.money(category.budget()));
      }
      case "budget" -> budget(a);
      case "income", "expense" -> {
        if (a.size() < 3 || a.size() > 5)
          usage(a.get(0) + " <категория> <сумма> [ГГГГ-ММ-ДД или -] [комментарий]");
        LocalDate date = a.size() >= 4 && !a.get(3).equals("-") ? LocalDate.parse(a.get(3)) : null;
        service.addOperation(
            a.get(0).equals("income") ? OperationType.INCOME : OperationType.EXPENSE,
            a.get(1),
            Validation.parseMoney(a.get(2), false),
            date,
            a.size() == 5 ? a.get(4) : "");
        output.println("Операция добавлена.");
        showAlerts();
      }
      case "transfer" -> {
        count(a, 3, "transfer <логин получателя> <сумма>");
        service.transfer(a.get(1), Validation.parseMoney(a.get(2), false));
        output.println("Перевод выполнен: " + a.get(1));
        showAlerts();
      }
      case "report", "history" -> {
        Options options = options(a, 1, true);
        var report = service.report(options.filter());
        String text =
            a.get(0).equals("report")
                ? ReportFormatter.summary(report)
                : ReportFormatter.history(report);
        if (options.file() == null) output.print(text);
        else {
          Files.writeString(
              options.file(), text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
          output.println("Отчёт записан: " + options.file());
        }
      }
      case "alerts" -> {
        count(a, 1, "alerts");
        showAlerts();
      }
      case "export" -> export(a);
      case "import" -> {
        count(a, 4, "import json <файл> --replace");
        if (!a.get(1).equals("json") || !a.get(3).equals("--replace"))
          usage("import json <файл> --replace");
        service.snapshot(); // Check authorization before reading a file.
        service.importSnapshot(json.read(Path.of(a.get(2)), WalletSnapshot.class));
        output.println("Кошелёк заменён данными снимка.");
        showAlerts();
      }
      case "save" -> {
        count(a, 1, "save");
        service.save();
        output.println("Все данные сохранены.");
      }
      case "exit", "quit" -> {
        count(a, 1, "exit");
        service.save();
        output.println("Данные сохранены. До свидания!");
        output.flush();
        return true;
      }
      default ->
          throw new IllegalArgumentException(
              "Неизвестная команда: " + a.get(0) + ". Введите help.");
    }
    return false;
  }

  private void category(List<String> a) {
    if (a.size() < 2) usage("category add|rename|delete ...");
    switch (a.get(1)) {
      case "add" -> {
        count(a, 3, "category add <название>");
        service.createCategory(a.get(2));
      }
      case "rename" -> {
        count(a, 4, "category rename <старое> <новое>");
        service.renameCategory(a.get(2), a.get(3));
      }
      case "delete" -> {
        count(a, 3, "category delete <название>");
        service.deleteCategory(a.get(2));
      }
      default -> usage("category add|rename|delete ...");
    }
    output.println("Категории обновлены.");
  }

  private void budget(List<String> a) {
    if (a.size() < 2) usage("budget set <категория> <лимит> | budget clear <категория>");
    switch (a.get(1)) {
      case "set" -> {
        count(a, 4, "budget set <категория> <лимит>");
        service.setBudget(a.get(2), Validation.parseMoney(a.get(3), true));
      }
      case "clear" -> {
        count(a, 3, "budget clear <категория>");
        service.setBudget(a.get(2), null);
      }
      default -> usage("budget set <категория> <лимит> | budget clear <категория>");
    }
    output.println("Бюджет обновлён.");
    showAlerts();
  }

  private void export(List<String> a) throws IOException {
    if (a.size() < 3) usage("export json|csv <файл> [фильтры для csv]");
    switch (a.get(1)) {
      case "json" -> {
        count(a, 3, "export json <файл>");
        json.export(Path.of(a.get(2)), service.snapshot());
      }
      case "csv" ->
          new CsvExporter()
              .export(Path.of(a.get(2)), service.report(options(a, 3, false).filter()));
      default -> usage("export json|csv <файл>");
    }
    output.println("Экспорт завершён: " + a.get(2));
  }

  private record Options(ReportFilter filter, Path file) {}

  private Options options(List<String> a, int start, boolean allowFile) {
    Set<String> categories = new HashSet<>();
    Set<String> seen = new HashSet<>();
    LocalDate from = null;
    LocalDate to = null;
    Path file = null;
    for (int i = start; i < a.size(); i += 2) {
      String option = a.get(i);
      if (i + 1 == a.size() || (!option.equals("--category") && !seen.add(option)))
        throw new IllegalArgumentException(
            "Отсутствует значение или повторяется параметр: " + option);
      String value = a.get(i + 1);
      switch (option) {
        case "--categories" -> categories.addAll(Arrays.asList(value.split(",", -1)));
        case "--category" -> categories.add(value);
        case "--from" -> from = LocalDate.parse(value);
        case "--to" -> to = LocalDate.parse(value);
        case "--file" -> {
          if (!allowFile) throw new IllegalArgumentException("Здесь --file не поддерживается.");
          file = Path.of(value);
        }
        default -> throw new IllegalArgumentException("Неизвестный параметр: " + option);
      }
    }
    return new Options(new ReportFilter(categories, from, to), file);
  }

  private void showAlerts() {
    List<String> alerts = service.alerts();
    if (alerts.isEmpty()) output.println("Оповещений нет.");
    else alerts.forEach(alert -> output.println("Внимание: " + alert));
  }

  private static void count(List<String> a, int size, String syntax) {
    if (a.size() != size) usage(syntax);
  }

  private static void usage(String syntax) {
    throw new IllegalArgumentException("Использование: " + syntax);
  }

  private static final String HELP =
      """
      register <логин>                  Зарегистрироваться; пароль вводится отдельно
      login <логин>                     Войти; пароль вводится отдельно
      logout                            Сохранить данные и выйти из аккаунта
      category add <название>            Создать категорию для доходов/расходов
      category rename <старое> <новое>   Переименовать, включая историю
      category delete <название>         Удалить категорию без операций
      categories                        Список категорий и лимитов
      budget set <категория> <лимит>     Установить/изменить бюджет (0 допустим)
      budget clear <категория>           Снять ограничение бюджета
      income <категория> <сумма> [дата|-] [комментарий]
      expense <категория> <сумма> [дата|-] [комментарий]
      transfer <логин> <сумма>           Перевести средства другому пользователю
      report [фильтры] [--file <файл>]    Сводка и бюджеты
      history [фильтры] [--file <файл>]   История операций
      alerts                            Текущие оповещения
      export json <файл>                Снимок кошелька без пароля
      export csv <файл> [фильтры]        Выгрузка операций
      import json <файл> --replace       Заменить текущий кошелёк снимком
      save                              Сохранить все аккаунты и кошельки
      exit / quit                       Сохранить и завершить приложение
      help                              Эта справка

      Фильтры: --categories "Еда,Такси" --from 2026-01-01 --to 2026-12-31
      Также: --category "Имя, с запятой"; параметр --category можно повторять.
      Даты включаются в выборку с обеих сторон. Без даты операция записывается сегодня.
      Названия с пробелами заключайте в кавычки: category add "Коммунальные услуги"
      Пример: expense "Коммунальные услуги" 3000 - "Платёж за квартиру"
      Суммы: 1500.50 или 1500,50, без разделителей тысяч. Единая валюта: RUB.
      Лимиты действуют за всю историю; фильтр дат меняет только отчёт.
      Экспорт создаёт новый файл; существующие файлы не перезаписываются.
      Импорт требует --replace и не может изменить операции переводов.
      """;
}
