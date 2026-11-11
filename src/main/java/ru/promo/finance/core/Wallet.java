package ru.promo.finance.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Aggregate: all changes preserve category references and operation identity. */
public final class Wallet {
  private final Map<String, Category> categories = new LinkedHashMap<>();
  private final List<Operation> operations = new ArrayList<>();
  private final Set<UUID> operationIds = new HashSet<>();

  public static Wallet restore(WalletSnapshot snapshot) {
    if (snapshot == null) throw new IllegalArgumentException("Кошелёк отсутствует.");
    Wallet wallet = new Wallet();
    for (Category category : snapshot.categories()) {
      wallet.createCategory(category.name());
      wallet.setBudget(category.name(), category.budget());
    }
    for (Operation operation : snapshot.operations()) wallet.add(operation);
    return wallet;
  }

  public WalletSnapshot snapshot() {
    return new WalletSnapshot(1, categories(), operations());
  }

  public List<Category> categories() {
    return List.copyOf(categories.values());
  }

  public List<Operation> operations() {
    return List.copyOf(operations);
  }

  public Category category(String name) {
    Category found = categories.get(Validation.key(name));
    if (found == null) throw new IllegalArgumentException("Категория не найдена: " + name);
    return found;
  }

  public boolean hasCategory(String name) {
    return categories.containsKey(Validation.key(name));
  }

  public void createCategory(String name) {
    Category category = new Category(name, null);
    String key = Validation.key(name);
    if (categories.containsKey(key))
      throw new IllegalArgumentException("Категория уже существует: " + name);
    categories.put(key, category);
  }

  public void renameCategory(String oldName, String newName) {
    Category old = category(oldName);
    Category renamed = new Category(newName, old.budget());
    String oldKey = Validation.key(oldName);
    String newKey = Validation.key(newName);
    if (!oldKey.equals(newKey) && categories.containsKey(newKey))
      throw new IllegalArgumentException("Категория уже существует: " + newName);
    Map<String, Category> updated = new LinkedHashMap<>();
    categories.forEach(
        (key, value) ->
            updated.put(key.equals(oldKey) ? newKey : key, key.equals(oldKey) ? renamed : value));
    categories.clear();
    categories.putAll(updated);
    operations.replaceAll(
        op -> Validation.key(op.category()).equals(oldKey) ? op.withCategory(renamed.name()) : op);
  }

  public void deleteCategory(String name) {
    Category existing = category(name);
    if (operations.stream()
        .anyMatch(op -> Validation.key(op.category()).equals(Validation.key(existing.name())))) {
      throw new IllegalArgumentException(
          "Нельзя удалить категорию с операциями. Используйте переименование.");
    }
    categories.remove(Validation.key(name));
  }

  public void setBudget(String name, BigDecimal amount) {
    Category old = category(name);
    categories.put(Validation.key(name), new Category(old.name(), amount));
  }

  public void add(Operation operation) {
    Category category = category(operation.category());
    if (!operationIds.add(operation.id()))
      throw new IllegalArgumentException("Повторяющийся идентификатор операции.");
    operations.add(operation.withCategory(category.name()));
  }

  public BigDecimal total(OperationType type) {
    return operations.stream()
        .filter(op -> op.type() == type)
        .map(Operation::amount)
        .reduce(new BigDecimal("0.00"), BigDecimal::add);
  }

  public BigDecimal balance() {
    return total(OperationType.INCOME).subtract(total(OperationType.EXPENSE));
  }
}
