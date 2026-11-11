package ru.promo.finance.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Application use cases and the current authenticated session. */
public final class FinanceService {
  public static final String TRANSFERS = "Переводы";
  private final StateRepository repository;
  private final PasswordHasher passwords;
  private final Clock clock;
  private final Map<String, AccountData> accounts = new LinkedHashMap<>();
  private final Map<String, Wallet> wallets = new LinkedHashMap<>();
  private String currentLogin;

  public FinanceService(StateRepository repository, PasswordHasher passwords, Clock clock)
      throws IOException {
    this.repository = repository;
    this.passwords = passwords;
    this.clock = clock;
    for (AccountData account : repository.load().accounts()) accounts.put(account.login(), account);
  }

  public String currentLogin() {
    return currentLogin;
  }

  public void register(String login, String password) {
    String key = Validation.login(login);
    if (password == null
        || password.isBlank()
        || password.length() < 8
        || password.length() > 128
        || password.chars().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException("Пароль: 8–128 символов без управляющих знаков.");
    if (accounts.containsKey(key)) throw new IllegalArgumentException("Логин уже занят.");
    PasswordHasher.Credential credential = passwords.hash(password);
    accounts.put(
        key, new AccountData(key, credential.salt(), credential.hash(), new Wallet().snapshot()));
  }

  public void login(String login, String password) {
    if (currentLogin != null) throw new IllegalArgumentException("Сначала выполните logout.");
    String key = Validation.login(login);
    AccountData account = accounts.get(key);
    if (account == null
        || password == null
        || password.length() > 128
        || !passwords.matches(password, account.salt(), account.passwordHash())) {
      throw new IllegalArgumentException("Неверный логин или пароль.");
    }
    loadWallet(key);
    currentLogin = key;
  }

  public void logout() throws IOException {
    requireLogin();
    save();
    currentLogin = null;
  }

  private Wallet loadWallet(String login) {
    return wallets.computeIfAbsent(login, key -> Wallet.restore(accounts.get(key).wallet()));
  }

  private void requireLogin() {
    if (currentLogin == null) throw new IllegalArgumentException("Сначала войдите: login <логин>.");
  }

  private Wallet wallet() {
    requireLogin();
    return loadWallet(currentLogin);
  }

  public WalletSnapshot snapshot() {
    return wallet().snapshot();
  }

  public List<Category> categories() {
    return wallet().categories();
  }

  public void createCategory(String name) {
    wallet().createCategory(name);
  }

  public void renameCategory(String oldName, String newName) {
    wallet().renameCategory(oldName, newName);
  }

  public void deleteCategory(String name) {
    wallet().deleteCategory(name);
  }

  public void setBudget(String category, BigDecimal amount) {
    wallet().setBudget(category, amount);
  }

  public void addOperation(
      OperationType type, String category, BigDecimal amount, LocalDate date, String note) {
    Wallet target = wallet();
    LocalDate actual = date == null ? LocalDate.now(clock) : date;
    if (actual.isAfter(LocalDate.now(clock)))
      throw new IllegalArgumentException("Дата операции не может быть в будущем.");
    target.add(new Operation(UUID.randomUUID(), actual, type, category, amount, note, null, null));
  }

  public void transfer(String recipient, BigDecimal amount) {
    Wallet sender = wallet();
    String targetLogin = Validation.login(recipient);
    BigDecimal value = Validation.money(amount, false);
    if (targetLogin.equals(currentLogin))
      throw new IllegalArgumentException("Нельзя переводить самому себе.");
    if (!accounts.containsKey(targetLogin))
      throw new IllegalArgumentException("Получатель не найден.");
    if (sender.balance().compareTo(value) < 0)
      throw new IllegalArgumentException("Недостаточно средств для перевода.");
    // Prepare both complete aggregates before committing either side in memory.
    Wallet senderCopy = Wallet.restore(sender.snapshot());
    Wallet receiverCopy = Wallet.restore(loadWallet(targetLogin).snapshot());
    if (!senderCopy.hasCategory(TRANSFERS)) senderCopy.createCategory(TRANSFERS);
    if (!receiverCopy.hasCategory(TRANSFERS)) receiverCopy.createCategory(TRANSFERS);
    UUID transferId = UUID.randomUUID();
    LocalDate today = LocalDate.now(clock);
    senderCopy.add(
        new Operation(
            UUID.randomUUID(),
            today,
            OperationType.EXPENSE,
            TRANSFERS,
            value,
            "Перевод пользователю " + targetLogin,
            targetLogin,
            transferId));
    receiverCopy.add(
        new Operation(
            UUID.randomUUID(),
            today,
            OperationType.INCOME,
            TRANSFERS,
            value,
            "Перевод от " + currentLogin,
            currentLogin,
            transferId));
    wallets.put(currentLogin, senderCopy);
    wallets.put(targetLogin, receiverCopy);
  }

  public ReportService.Report report(ReportFilter filter) {
    return new ReportService().calculate(wallet(), filter);
  }

  public List<String> alerts() {
    return new AlertService().alerts(wallet());
  }

  public void importSnapshot(WalletSnapshot snapshot) {
    requireLogin();
    Wallet candidate = Wallet.restore(snapshot);
    if (candidate.operations().stream().anyMatch(op -> op.date().isAfter(LocalDate.now(clock))))
      throw new IllegalArgumentException("Снимок содержит операции из будущего.");
    // A local restore must never create or erase one side of an inter-user transfer.
    List<Operation> oldTransfers =
        wallet().operations().stream().filter(op -> op.transferId() != null).toList();
    List<Operation> newTransfers =
        candidate.operations().stream().filter(op -> op.transferId() != null).toList();
    if (!oldTransfers.equals(newTransfers))
      throw new IllegalArgumentException(
          "Импорт не должен менять операции переводов. Для полного восстановления используйте резервную копию finance.json.");
    wallets.put(currentLogin, candidate);
  }

  public void save() throws IOException {
    List<AccountData> updated =
        accounts.values().stream()
            .map(
                account ->
                    new AccountData(
                        account.login(),
                        account.salt(),
                        account.passwordHash(),
                        wallets.containsKey(account.login())
                            ? wallets.get(account.login()).snapshot()
                            : account.wallet()))
            .toList();
    repository.save(new ApplicationState(1, updated));
  }
}
