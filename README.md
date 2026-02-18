# Структура проекта News Aggregator

## Корневые файлы репозитория
- `pom.xml` - Maven-конфигурация: зависимости Spring Boot, MySQL, Thymeleaf, JSoup
- `mvnw`, `mvnw.cmd` - Maven Wrapper для запуска без глобальной установки Maven
- `monitor-logs.sh` - скрипт мониторинга логов приложения
- `manual_parse_rss.sh` - скрипт ручного парсинга RSS лент
- `test_admin_article_view.sh` - скрипт тестирования просмотра статей админом

## Java-код (`src/main/java/com/newsaggregator`)

### Главный класс
- `NewsAggregatorApplication.java` - точка входа Spring Boot приложения

### Пакет `config` (Конфигурация)
- `SecurityConfig.java` - настройки Spring Security, аутентификация, авторизация
- `DataInitializer.java` - инициализация базовых данных при старте (роли, категории, источники)
- `CaptchaAuthenticationFilter.java` - фильтр проверки CAPTCHA при входе

### Пакет `controller` (MVC слой)
- `HomeController.java` - главная страница, навигация по категориям
- `AdminController.java` - административная панель, управление статьями/пользователями
- `EditorController.java` - панель редактора, модерация статей
- `UserController.java` - пользовательские функции, профиль, настройки
- `CustomErrorController.java` - обработка ошибок 404, 500
- `GlobalControllerAdvice.java` - глобальная обработка исключений
- `TestController.java` - тестовые страницы для AI интеграции

### Пакет `entity` (Модели данных)
- `User.java` - пользователь системы
- `Role.java` - роли пользователей (ADMIN, EDITOR, READER)
- `Article.java` - статья новостей
- `Category.java` - категория новостей
- `NewsSource.java` - источник RSS лент
- `SavedArticle.java` - сохраненные пользователем статьи
- `UserPreferences.java` - пользовательские настройки
- `ArticleStatus.java` - статусы статей (DRAFT, PUBLISHED, REJECTED)

### Пакет `repository` (Доступ к данным)
- `UserRepository.java` - операции с пользователями
- `RoleRepository.java` - операции с ролями
- `ArticleRepository.java` - операции со статьями + поиск и фильтрация
- `CategoryRepository.java` - операции с категориями
- `NewsSourceRepository.java` - операции с источниками новостей
- `SavedArticleRepository.java` - операции с сохраненными статьями
- `UserPreferencesRepository.java` - операции с настройками пользователей

### Пакет `service` (Бизнес-логика)
- `ArticleService.java` - основные операции со статьями, массовые действия
- `UserService.java` - операции с пользователями, регистрация
- `CategoryService.java` - операции с категориями
- `NewsSourceService.java` - операции с источниками новостей
- `SavedArticleService.java` - операции с сохраненными статьями
- `RssParserService.java` - парсинг RSS лент, извлечение статей
- `RssSchedulerService.java` - планировщик автоматического парсинга
- `ArticleContentExtractorService.java` - извлечение полного текста статей с сайтов
- `LMStudioService.java` - интеграция с LM Studio для генерации кратких содержаний
- `CaptchaService.java` - генерация и проверка CAPTCHA

### Пакет `util`
- `Breadcrumb.java` - утилита для навигационных хлебных крошек

## Ресурсы (`src/main/resources`)

### Конфигурация
- `application.yml` - основные настройки приложения, профили, логирование
- `logback-spring.xml` - детальная конфигурация системы логирования

### Статические ресурсы (`static`)
- `css/style.css` - основные стили приложения
- `js/app.js` - клиентские JavaScript скрипты
- `images/articles/` - изображения для статей (SVG иконки)

### Шаблоны (`templates`)
- `index.html` - главная страница
- `login.html` - страница входа
- `about.html` - страница "О нас"
- `error.html` - страница ошибок
- `admin/` - административные шаблоны (dashboard, articles, users, categories, sources)
- `editor/` - шаблоны редактора (dashboard, moderate, edit-article, stats)
- `user/` - пользовательские шаблоны (profile, preferences, saved)
- `news/` - шаблоны новостей (list, view, category, search)
- `auth/` - шаблоны аутентификации (register)
- `test/` - тестовые шаблоны (ai)
- `layout/` - базовые шаблоны и фрагменты

## База данных (`database`)
- `create_database.sql` - создание базы данных и таблиц
- `drop_database.sql` - удаление базы данных
- `add_saved_articles_table.sql` - добавление таблицы сохраненных статей
- `insert_test_data.sql` - вставка тестовых данных
- `README.md` - документация по базе данных

## Тестирование (`src/test/java`)
- `functional/` - функциональные тесты (ArticleWorkflowTest, UserManagementWorkflowTest)
- `service/` - unit-тесты сервисов
- `application-test.yml` - конфигурация для тестов

## Логирование (`logs`)
- `news-aggregator.log` - основной лог приложения
- `rss-parser.log` - логи парсинга RSS
- `ai-service.log` - логи AI сервиса
- `news-aggregator-error.log` - логи ошибок

## Спецификации (`.kiro/specs`)
- Содержит спецификации для различных улучшений системы
- Структурированные требования и планы развития

## Основные функции системы
1. **Парсинг RSS** - автоматическое получение новостей из источников
2. **Модерация контента** - редакторы проверяют и публикуют статьи
3. **Пользовательские функции** - сохранение статей, настройки
4. **Административная панель** - управление пользователями и контентом
5. **AI интеграция** - генерация кратких содержаний через LM Studio
6. **Система ролей** - ADMIN, EDITOR, READER с разными правами
7. **Безопасность** - Spring Security + CAPTCHA защита
8. **Логирование** - детальное логирование всех операций

## Технологический стек
- **Backend**: Spring Boot 3.2.1, Spring Security, Spring Data JPA
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **База данных**: MySQL 8.0
- **Парсинг**: JSoup для извлечения контента
- **AI**: LM Studio интеграция
- **Сборка**: Maven
- **Логирование**: Logback