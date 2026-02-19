# Docker развертывание News Aggregator

## Быстрый старт

### 1. Сборка и запуск (локальная сборка)
```bash
# Сборка образа и запуск контейнеров
docker-compose up -d

# Просмотр логов
docker-compose logs -f app

# Остановка
docker-compose down
```

### 2. Использование образа с Docker Hub
Если образ уже опубликован на Docker Hub:

```bash
# 1. Отредактируйте docker-compose.yml
# Закомментируйте секцию build и раскомментируйте image:
#   build:
#     context: .
#     dockerfile: Dockerfile
#   image: YOUR_USERNAME/news-aggregator:latest  # Раскомментируйте

# 2. Запустите
docker-compose up -d
```

### 3. Доступ к приложению
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

## Публикация на Docker Hub

### Ручная публикация

1. **Войдите в Docker Hub**:
```bash
docker login
```

2. **Соберите образ с тегом**:
```bash
# Замените YOUR_USERNAME на ваш Docker Hub username
docker build -t YOUR_USERNAME/news-aggregator:1.0.0 .
docker tag YOUR_USERNAME/news-aggregator:1.0.0 YOUR_USERNAME/news-aggregator:latest
```

3. **Запушьте образ**:
```bash
docker push YOUR_USERNAME/news-aggregator:1.0.0
docker push YOUR_USERNAME/news-aggregator:latest
```

4. **Используйте опубликованный образ**:
```bash
docker pull YOUR_USERNAME/news-aggregator:latest
```

### Автоматическая публикация (скрипт)

Используйте готовый скрипт `docker-push.sh`:

```bash
# 1. Отредактируйте скрипт - замените YOUR_DOCKERHUB_USERNAME
nano docker-push.sh

# 2. Запустите скрипт
./docker-push.sh
```

### Использование опубликованного образа

После публикации обновите `docker-compose.yml`:

```yaml
services:
  app:
    image: YOUR_USERNAME/news-aggregator:latest  # Вместо build
    # build:
    #   context: .
    #   dockerfile: Dockerfile
```

## Интеграция с CI/CD

### GitHub Actions пример
```yaml
name: Build and Push Docker Image

on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: YOUR_USERNAME/news-aggregator
          tags: |
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
      
      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
```

## Поддержка

При возникновении проблем:
1. Проверьте логи: `docker-compose logs`
2. Проверьте health checks: `docker-compose ps`
3. Проверьте ресурсы: `docker stats`
