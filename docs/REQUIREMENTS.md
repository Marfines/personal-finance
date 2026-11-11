# Соответствие критериям

Таблица указывает реализацию и проверку; итоговую оценку выставляет ментор.

| № | Критерий | Реализация | Проверка |
| --- | --- | --- | --- |
| 1 | Авторизация и несколько пользователей | `FinanceService`, `PasswordHasher`, `register/login/logout` | FinanceServiceTest, PersistenceTest |
| 2 | CLI и цикл | `FinanceCli`, help, exit, quit, EOF | FinanceCliTest |
| 3 | Доходы и расходы | `Operation`, `Wallet`, команды income/expense | WalletTest, пример ТЗ |
| 4 | Кошелёк пользователя | Сессия по логину, вычисляемый баланс, полная история | Тесты изоляции и перезапуска |
| 5 | Категории и бюджеты | Создание категорий, set/clear бюджета, остаток | WalletTest, FinanceServiceTest |
| 6 | Сводки | Таблица доходов, расходов, лимитов и остатков | Пример 63000 / 8300 / -500 |
| 7 | Оповещения | `AlertService`, проверка после изменения состояния | AlertServiceTest |
| 8 | Сохранение и загрузка | Единый атомарный JSON, восстановление кошелька при входе | PersistenceTest, EOF/exit |
| 9 | Валидация | Модель + синтаксис CLI, понятные сообщения | WalletTest, FinanceCliTest |
| 10 | Разделение классов | core / infra / cli | ARCHITECTURE.md |
| 11 | Выборки | Несколько категорий, даты включительно, пустая выборка | WalletTest, FinanceCliTest |
| 12 | Редактирование | Переименование/удаление пустых категорий, изменение/снятие бюджета | WalletTest |
| 13 | Экспорт и импорт | JSON в обе стороны, CSV и текстовые отчёты на выход | PersistenceTest, FinanceServiceTest |
| 14 | Расширенные уведомления | 80%, исчерпание, перерасход, нулевой баланс | AlertServiceTest |
| 15 | UX CLI | help, примеры, кавычки, форматированные таблицы, ошибки | FinanceCliTest, demo.commands |
| 16 | Тестирование | Более 15 unit- и интеграционных тестов | `mvnw verify`, surefire-reports |
| 17 | Архитектура | Доменные правила, порты репозитория/паролей, DI, Clock | ARCHITECTURE.md, тестовые адаптеры |
| 18 | Сборка и CI | Maven Wrapper, Shade, Spotless, GitHub Actions | `verify`, .github/workflows/ci.yml |
| 19 | Документация | README, описание модулей, тестов, GitHub | docs/ |
| Доп. | Переводы | Две связанные операции, логины, проверка средств, единый снимок | FinanceServiceTest, PersistenceTest |

Пример задания автоматически проверяется в `FinanceServiceTest.reproducesSpecificationExample`. Повторить его вручную можно по `examples/demo.commands`.
