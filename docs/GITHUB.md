# Публикация и сдача

Репозиторий проекта: [Marfines/personal-finance](https://github.com/Marfines/personal-finance). Для сдачи ментору используйте эту ссылку. Текущая проверка доступна на [вкладке Actions](https://github.com/Marfines/personal-finance/actions).

Для первоначальной публикации собственной копии из корня проекта:

```powershell
.\mvnw.cmd verify
git init -b main
git add .
git status
git commit -m "Implement personal finance CLI"
git remote add origin https://github.com/YOUR_LOGIN/personal-finance.git
git push -u origin main
```

Замените `YOUR_LOGIN` на свой GitHub-логин. Если Git запросит имя и email автора, настройте их для этого репозитория: `git config user.name "Ваше имя"` и `git config user.email "Ваш email"`. При push используйте штатную авторизацию GitHub через Git Credential Manager или SSH; токены не добавляйте в файлы проекта.

Перед коммитом `git status` должен показывать код, документацию, wrapper и workflow. Каталоги `data`, `target`, `.tools`, `exports` игнорируются. Личные экспорты в корне проекта тоже не добавляйте; храните их в `exports/`.

После публикации:

1. Дождитесь успешного запуска **Java CI** на вкладке Actions.
2. Проверьте, что README открывается и содержит команды сборки и тестирования.
3. Откройте репозиторий в новой папке и повторите сборку; все необходимые зависимости скачиваются автоматически.
4. Отправьте ссылку `https://github.com/Marfines/personal-finance` через форму сдачи курса.

В комментарии к сдаче можно указать: «Реализованы основные и расширенные функции, переводы, автоматические тесты, Maven Wrapper и CI; пример из ТЗ находится в examples/demo.commands».
