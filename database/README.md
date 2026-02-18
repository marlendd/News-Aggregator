# База данных для Агрегатора новостей

Этот каталог содержит SQL скрипты для создания и настройки базы данных MySQL для приложения "Агрегатор новостей".

## Файлы

- `create_database.sql` - Основной скрипт создания базы данных и таблиц
- `insert_test_data.sql` - Скрипт для вставки тестовых данных
- `drop_database.sql` - Скрипт для удаления базы данных (осторожно!)

## Быстрый старт

### 1. Установка MySQL

Убедитесь, что у вас установлен MySQL Server 8.0 или выше.

**Windows:**
```bash
# Скачайте MySQL Installer с официального сайта
# https://dev.mysql.com/downloads/installer/
```

**macOS:**
```bash
brew install mysql
brew services start mysql
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

### 2. Создание базы данных

Подключитесь к MySQL как root пользователь:

```bash
mysql -u root -p
```

Выполните основной скрипт создания:

```sql
source /path/to/database/create_database.sql
```

Или скопируйте содержимое файла и выполните в MySQL клиенте.

### 3. Добавление тестовых данных (опционально)

Для демонстрации приложения можно добавить тестовые статьи:

```sql
source /path/to/database/insert_test_data.sql
```

### 4. Настройка приложения

Убедитесь, что в файле `src/main/resources/application.yml` указаны правильные параметры подключения:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/news_aggregator?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_mysql_password
```

## Структура базы данных

### Основные таблицы:

1. **users** - Пользователи системы
2. **roles** - Роли пользователей (ADMIN, EDITOR, READER)
3. **user_roles** - Связь пользователей и ролей
4. **categories** - Категории новостей
5. **news_sources** - RSS источники новостей
6. **articles** - Статьи
7. **user_preferences** - Персональные настройки пользователей
8. **saved_articles** - Сохраненные пользователями статьи

### Диаграмма связей:

```
users ←→ user_roles ←→ roles
  ↓
user_preferences
  ↓
saved_articles ←→ articles ←→ categories
                     ↓
                news_sources
```

## Базовые данные

После выполнения скрипта создания будут созданы:

### Роли:
- `ADMIN` - Администратор системы
- `EDITOR` - Редактор контента  
- `READER` - Читатель

### Категории:
- Технологии (#007bff)
- Политика (#dc3545)
- Экономика (#28a745)
- Спорт (#fd7e14)
- Наука (#6f42c1)
- Культура (#e83e8c)
- Здоровье (#20c997)
- Образование (#6c757d)
- Общество (#ffc107)
- Мир (#17a2b8)

### RSS источники:
- Хабр (https://habr.com/ru/rss/hub/programming/)
- TechCrunch (https://techcrunch.com/feed/)
- BBC News (http://feeds.bbci.co.uk/news/rss.xml)
- Reuters (https://www.reutersagency.com/feed/?best-topics=tech)
- Ведомости (https://www.vedomosti.ru/rss/news)

## Тестовые пользователи

Пользователи создаются автоматически при первом запуске приложения через `DataInitializer`:

- **admin** / admin123 (роль: ADMIN)
- **editor** / editor123 (роль: EDITOR)
- **reader** / reader123 (роль: READER)

## Индексы и оптимизация

База данных включает следующие индексы для оптимизации производительности:

- Индексы на часто используемые поля (username, email, status)
- Полнотекстовый индекс для поиска по статьям
- Внешние ключи с каскадным удалением где необходимо

## Резервное копирование

Для создания резервной копии:

```bash
mysqldump -u root -p news_aggregator > backup_$(date +%Y%m%d_%H%M%S).sql
```

Для восстановления:

```bash
mysql -u root -p news_aggregator < backup_file.sql
```

## Устранение неполадок

### Ошибка подключения
- Проверьте, что MySQL сервер запущен
- Убедитесь в правильности пароля root пользователя
- Проверьте настройки firewall

### Ошибки кодировки
- Убедитесь, что используется кодировка utf8mb4
- Проверьте настройки MySQL сервера

### Проблемы с правами доступа
```sql
GRANT ALL PRIVILEGES ON news_aggregator.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

## Дополнительная настройка

### Создание отдельного пользователя для приложения:

```sql
CREATE USER 'news_app'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON news_aggregator.* TO 'news_app'@'localhost';
FLUSH PRIVILEGES;
```

Затем обновите `application.yml`:

```yaml
spring:
  datasource:
    username: news_app
    password: secure_password
```