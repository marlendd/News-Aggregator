# Многоэтапная сборка для оптимизации размера образа

# Этап 1: Сборка приложения
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем pom.xml и загружаем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем приложение
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Финальный образ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Создаем пользователя для запуска приложения
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем JAR из этапа сборки
COPY --from=build /app/target/*.jar app.jar

# Создаем директорию для логов
RUN mkdir -p /app/logs

# Открываем порт приложения
EXPOSE 8080

# Настройки JVM для контейнера
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
