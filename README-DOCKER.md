# Docker развертывание News Aggregator

## Быстрый старт (Development)

### 1. Сборка и запуск
```bash
# Сборка образа и запуск контейнеров
docker-compose up -d

# Просмотр логов
docker-compose logs -f app

# Остановка
docker-compose down
```

### 2. Доступ к приложению
- **Приложение**: http://localhost:8080
- **MySQL**: localhost:3306

### 3. Учетные данные по умолчанию
- **Admin**: admin / admin123
- **Editor**: editor / editor123
- **Reader**: reader / reader123

## Production развертывание

### 1. Подготовка
```bash
# Создайте .env файл из примера
cp .env.example .env

# Отредактируйте .env и установите безопасные пароли
nano .env
```

### 2. Запуск production
```bash
# Запуск с production конфигурацией
docker-compose -f docker-compose.prod.yml up -d

# Проверка статуса
docker-compose -f docker-compose.prod.yml ps

# Просмотр логов
docker-compose -f docker-compose.prod.yml logs -f
```

## Полезные команды

### Управление контейнерами
```bash
# Перезапуск приложения
docker-compose restart app

# Пересборка образа
docker-compose build --no-cache app

# Остановка и удаление всех контейнеров
docker-compose down -v
```

### Логи и отладка
```bash
# Логи приложения
docker-compose logs -f app

# Логи MySQL
docker-compose logs -f mysql

# Вход в контейнер приложения
docker-compose exec app sh

# Вход в MySQL
docker-compose exec mysql mysql -u news_user -p news_aggregator
```

### Резервное копирование

#### Backup базы данных
```bash
# Создание backup
docker-compose exec mysql mysqldump -u news_user -p news_aggregator > backup_$(date +%Y%m%d_%H%M%S).sql

# Восстановление из backup
docker-compose exec -T mysql mysql -u news_user -p news_aggregator < backup_20240101_120000.sql
```

#### Backup volumes
```bash
# Backup MySQL данных
docker run --rm -v news-aggregator_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_backup.tar.gz -C /data .

# Восстановление
docker run --rm -v news-aggregator_mysql_data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql_backup.tar.gz -C /data
```

## Мониторинг

### Проверка здоровья
```bash
# Статус контейнеров
docker-compose ps

# Health check приложения
curl http://localhost:8080/actuator/health

# Использование ресурсов
docker stats
```

### Метрики
```bash
# Метрики Spring Boot Actuator
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## Масштабирование

### Запуск нескольких экземпляров приложения
```bash
# Масштабирование до 3 экземпляров
docker-compose up -d --scale app=3

# Требуется настроить load balancer (nginx/traefik)
```

## Troubleshooting

### Приложение не запускается
```bash
# Проверьте логи
docker-compose logs app

# Проверьте подключение к MySQL
docker-compose exec app sh
wget -O- http://mysql:3306
```

### MySQL проблемы
```bash
# Проверьте статус MySQL
docker-compose exec mysql mysqladmin -u root -p status

# Проверьте логи MySQL
docker-compose logs mysql
```

### Очистка и перезапуск
```bash
# Полная очистка (ВНИМАНИЕ: удалит все данные!)
docker-compose down -v
docker system prune -a
docker-compose up -d
```

## Обновление приложения

```bash
# 1. Остановка текущей версии
docker-compose down

# 2. Получение новой версии кода
git pull

# 3. Пересборка образа
docker-compose build --no-cache app

# 4. Запуск новой версии
docker-compose up -d

# 5. Проверка логов
docker-compose logs -f app
```

## Переменные окружения

### Основные настройки
- `SPRING_DATASOURCE_URL` - URL подключения к MySQL
- `SPRING_DATASOURCE_USERNAME` - Пользователь БД
- `SPRING_DATASOURCE_PASSWORD` - Пароль БД
- `SPRING_JPA_HIBERNATE_DDL_AUTO` - Режим Hibernate (update/validate)
- `JAVA_OPTS` - Параметры JVM

### Настройки приложения
- `APP_RSS_UPDATE_INTERVAL` - Интервал обновления RSS (мс)
- `APP_LMSTUDIO_ENABLED` - Включить LM Studio
- `LOGGING_LEVEL_ROOT` - Уровень логирования

## Безопасность

### Рекомендации для production:
1. Используйте сильные пароли в `.env`
2. Не публикуйте порт MySQL (3306) наружу
3. Настройте SSL для MySQL
4. Используйте secrets для паролей
5. Регулярно обновляйте образы
6. Настройте firewall

### Использование Docker secrets
```yaml
# В docker-compose.prod.yml
secrets:
  mysql_password:
    file: ./secrets/mysql_password.txt
```

## Производительность

### Оптимизация JVM
```bash
# Для 2GB RAM
JAVA_OPTS="-Xmx1536m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Для 4GB RAM
JAVA_OPTS="-Xmx3072m -Xms1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Оптимизация MySQL
Создайте `mysql-conf/my.cnf`:
```ini
[mysqld]
max_connections=200
innodb_buffer_pool_size=512M
innodb_log_file_size=128M
```

## Интеграция с CI/CD

### GitHub Actions пример
```yaml
- name: Build Docker image
  run: docker build -t news-aggregator:latest .

- name: Push to registry
  run: |
    docker tag news-aggregator:latest registry.example.com/news-aggregator:latest
    docker push registry.example.com/news-aggregator:latest
```

## Поддержка

При возникновении проблем:
1. Проверьте логи: `docker-compose logs`
2. Проверьте health checks: `docker-compose ps`
3. Проверьте ресурсы: `docker stats`
