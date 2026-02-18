package com.newsaggregator.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Сервис для интеграции с LM Studio API для ИИ-обработки статей.
 * 
 * Предоставляет функциональность для:
 * - Автоматической категоризации статей с помощью ИИ
 * - Генерации кратких сводок статей
 * - Взаимодействия с локальным LM Studio API
 * - Fallback на базовые алгоритмы при недоступности ИИ
 * - Проверки доступности и настройки API
 * - Получения списка доступных моделей
 * - Валидации качества сгенерированного контента
 * 
 * Поддерживает работу как с ИИ, так и без него (graceful degradation).
 * 
 * @author News Aggregator Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class LMStudioService {

    private static final Logger logger = LoggerFactory.getLogger(LMStudioService.class);

    @Value("${app.lmstudio.api-url:http://localhost:1234/v1}")
    private String apiUrl;

    @Value("${app.lmstudio.enabled:false}")
    private boolean enabled;

    @Value("${app.lmstudio.model:}")
    private String model;

    @Value("${app.lmstudio.timeout:60}")
    private int timeoutSeconds;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Категории для классификации
    private static final List<String> CATEGORIES = Arrays.asList(
        "Технологии",
        "Спорт", 
        "Политика",
        "Экономика",
        "Наука",
        "Культура",
        "Общество",
        "Здоровье",
        "Образование",
        "Путешествия"
    );

    public LMStudioService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Автоматическая категоризация статьи с помощью LM Studio
     */
    public String categorizeArticle(String title, String content) {
        logger.info("Категоризация статьи с помощью LM Studio: {}", title);
        
        if (!enabled) {
            logger.info("LM Studio отключен, используется базовая категоризация");
            return determineBasicCategory(title, content);
        }
        
        try {
            String text = prepareTextForClassification(title, content);
            String category = classifyText(text);
            
            logger.info("Определена категория: {} для статьи: {}", category, title);
            return category;
        } catch (Exception e) {
            logger.error("Ошибка при категоризации статьи '{}': {}", title, e.getMessage());
            return determineBasicCategory(title, content);
        }
    }

    /**
     * Генерация краткой сводки статьи с помощью LM Studio
     */
    public String generateSummary(String content) {
        logger.info("Генерация сводки с помощью LM Studio для статьи длиной {} символов", content.length());
        
        if (!enabled) {
            logger.info("LM Studio отключен, используется базовая генерация сводки");
            return generateBasicSummary(content);
        }
        
        try {
            String summary = summarizeText(content);
            
            // Проверяем качество сгенерированной сводки
            if (isValidSummary(summary, content)) {
                logger.info("Сгенерирована качественная сводка длиной {} символов", summary.length());
                return summary;
            } else {
                logger.warn("ИИ сгенерировал некачественную сводку, используем базовый метод");
                return generateBasicSummary(content);
            }
        } catch (Exception e) {
            logger.error("Ошибка при генерации сводки: {}", e.getMessage());
            return generateBasicSummary(content);
        }
    }

    /**
     * Классификация текста с помощью LM Studio
     */
    private String classifyText(String text) {
        try {
            String prompt = buildClassificationPrompt(text);
            String response = callLMStudio(prompt, 50);
            return extractCategory(response);
        } catch (Exception e) {
            logger.error("Ошибка при классификации текста: {}", e.getMessage());
            throw new RuntimeException("Ошибка классификации", e);
        }
    }

    /**
     * Суммаризация текста с помощью LM Studio
     */
    private String summarizeText(String content) {
        try {
            // Берем меньше контента для более краткой сводки (до 2000 символов)
            String truncatedContent = content.length() > 2000 ? content.substring(0, 2000) : content;
            String prompt = buildSummarizationPrompt(truncatedContent);
            // Уменьшаем лимит токенов для более краткой сводки
            String response = callLMStudio(prompt, 200);
            return cleanSummary(response);
        } catch (Exception e) {
            logger.error("Ошибка при суммаризации текста: {}", e.getMessage());
            throw new RuntimeException("Ошибка суммаризации", e);
        }
    }

    /**
     * Вызов LM Studio API
     */
    private String callLMStudio(String prompt, int maxTokens) {
        try {
            Map<String, Object> requestBody = Map.of(
                "model", model.isEmpty() ? "local-model" : model,
                "messages", List.of(
                    Map.of("role", "system", "content", "Ты - помощник для анализа новостных статей на русском языке."),
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", maxTokens
            );

            logger.debug("Отправка запроса в LM Studio: {}", apiUrl);

            String response = webClient.post()
                    .uri(apiUrl + "/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            return extractContent(response);
        } catch (WebClientResponseException e) {
            logger.error("Ошибка HTTP при вызове LM Studio: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Ошибка вызова LM Studio API", e);
        } catch (Exception e) {
            logger.error("Ошибка при вызове LM Studio: {}", e.getMessage());
            throw new RuntimeException("Ошибка вызова LM Studio", e);
        }
    }

    /**
     * Построение промпта для классификации
     */
    private String buildClassificationPrompt(String text) {
        return String.format(
            "Определи категорию для следующей новостной статьи. " +
            "Выбери ОДНУ категорию из списка: %s.\n\n" +
            "Статья: %s\n\n" +
            "Ответь ТОЛЬКО названием категории, без дополнительных объяснений.",
            String.join(", ", CATEGORIES),
            text
        );
    }

    /**
     * Построение промпта для суммаризации
     */
    private String buildSummarizationPrompt(String content) {
        return String.format(
            "Создай очень краткую сводку следующей новостной статьи на русском языке. " +
            "Сводка должна быть максимально сжатой и содержать только главную суть. " +
            "Длина сводки: 1-2 предложения (максимум 150 символов). " +
            "Не добавляй вводные фразы типа 'В статье говорится' или 'Сводка:'. " +
            "Начинай сразу с сути.\n\n" +
            "Статья: %s\n\n" +
            "Краткая сводка:",
            content
        );
    }

    /**
     * Извлечение контента из ответа LM Studio
     */
    private String extractContent(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode choices = jsonNode.path("choices");
            
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0)
                    .path("message")
                    .path("content")
                    .asText();
                return content.trim();
            }
            
            throw new RuntimeException("Не удалось извлечь контент из ответа");
        } catch (Exception e) {
            logger.error("Ошибка при извлечении контента: {}", e.getMessage());
            throw new RuntimeException("Не удалось извлечь контент", e);
        }
    }

    /**
     * Извлечение категории из ответа
     */
    private String extractCategory(String response) {
        String cleaned = response.trim();
        
        // Ищем категорию в ответе
        for (String category : CATEGORIES) {
            if (cleaned.toLowerCase().contains(category.toLowerCase())) {
                return category;
            }
        }
        
        // Если не нашли, возвращаем первое слово или дефолт
        String[] words = cleaned.split("\\s+");
        if (words.length > 0) {
            String firstWord = words[0].replaceAll("[^а-яА-Я]", "");
            for (String category : CATEGORIES) {
                if (category.toLowerCase().startsWith(firstWord.toLowerCase())) {
                    return category;
                }
            }
        }
        
        return "Общество";
    }

    /**
     * Очистка сводки от лишнего текста
     */
    private String cleanSummary(String summary) {
        if (summary == null || summary.isEmpty()) {
            return "";
        }
        
        // Убираем возможные префиксы
        summary = summary.replaceAll("^(Сводка|Summary|Краткое содержание|Краткая сводка|В статье|Статья):\\s*", "");
        summary = summary.replaceAll("^(В статье говорится|Статья рассказывает|В материале)\\s+(о том,?\\s+)?что\\s+", "");
        
        // Убираем кавычки в начале и конце
        summary = summary.replaceAll("^[\"«]|[»\"]$", "");
        
        summary = summary.trim();
        
        // Ограничиваем длину (до 200 символов для краткой сводки)
        if (summary.length() > 200) {
            summary = summary.substring(0, 200);
            // Обрезаем по последней точке, если она есть после 100 символов
            int lastPeriod = summary.lastIndexOf('.');
            if (lastPeriod > 100) {
                summary = summary.substring(0, lastPeriod + 1);
            } else {
                // Если точки нет, обрезаем по последнему пробелу
                int lastSpace = summary.lastIndexOf(' ');
                if (lastSpace > 100) {
                    summary = summary.substring(0, lastSpace) + "...";
                }
            }
        }
        
        return summary;
    }

    /**
     * Подготовка текста для классификации
     */
    private String prepareTextForClassification(String title, String content) {
        String text = title;
        if (content != null && !content.isEmpty()) {
            String truncatedContent = content.length() > 500 ? content.substring(0, 500) : content;
            text = title + ". " + truncatedContent;
        }
        return text;
    }

    /**
     * Базовая категоризация как fallback
     */
    private String determineBasicCategory(String title, String content) {
        String text = (title + " " + (content != null ? content : "")).toLowerCase();
        
        if (containsKeywords(text, "технолог", "программ", "компьютер", "софт", "it", "интернет", "цифров")) {
            return "Технологии";
        }
        if (containsKeywords(text, "политик", "выбор", "правительств", "президент", "министр", "парламент")) {
            return "Политика";
        }
        if (containsKeywords(text, "экономик", "финанс", "банк", "рубль", "доллар", "бизнес", "торговл")) {
            return "Экономика";
        }
        if (containsKeywords(text, "спорт", "футбол", "хоккей", "олимпиад", "чемпионат", "матч")) {
            return "Спорт";
        }
        if (containsKeywords(text, "наук", "исследован", "открыт", "ученые", "эксперимент", "лаборатор")) {
            return "Наука";
        }
        if (containsKeywords(text, "здоровь", "медицин", "врач", "лечен", "болезн", "клиник")) {
            return "Здоровье";
        }
        if (containsKeywords(text, "культур", "искусств", "театр", "кино", "музык", "выставк")) {
            return "Культура";
        }
        if (containsKeywords(text, "образован", "школ", "университет", "студент", "учител", "экзамен")) {
            return "Образование";
        }
        if (containsKeywords(text, "путешеств", "туризм", "отдых", "курорт", "экскурси")) {
            return "Путешествия";
        }
        
        return "Общество";
    }

    /**
     * Базовая генерация сводки как fallback
     */
    private String generateBasicSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        // Если контент короткий, возвращаем как есть
        if (content.length() <= 200) {
            return content;
        }
        
        // Пытаемся взять первые 1-2 предложения (до 200 символов)
        String summary = content.substring(0, Math.min(200, content.length()));
        
        // Ищем конец первого или второго предложения
        int sentenceCount = 0;
        int lastPeriod = -1;
        
        for (int i = 0; i < summary.length(); i++) {
            char c = summary.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                // Проверяем, что это не сокращение (например "т.д.")
                if (i + 1 < summary.length() && Character.isWhitespace(summary.charAt(i + 1))) {
                    sentenceCount++;
                    lastPeriod = i;
                    
                    // Берем 1-2 предложения, но не меньше 80 символов
                    if (sentenceCount >= 1 && lastPeriod >= 80) {
                        return summary.substring(0, lastPeriod + 1);
                    }
                    if (sentenceCount >= 2) {
                        return summary.substring(0, lastPeriod + 1);
                    }
                }
            }
        }
        
        // Если не нашли подходящих предложений, обрезаем по последней точке
        if (lastPeriod > 80) {
            return summary.substring(0, lastPeriod + 1);
        }
        
        // В крайнем случае обрезаем по последнему пробелу
        int lastSpace = summary.lastIndexOf(' ');
        if (lastSpace > 80) {
            return summary.substring(0, lastSpace) + "...";
        }
        
        return summary + "...";
    }
    
    /**
     * Проверка качества сгенерированной сводки
     */
    private boolean isValidSummary(String summary, String originalContent) {
        if (summary == null || summary.trim().isEmpty()) {
            return false;
        }
        
        summary = summary.trim();
        
        // Проверяем минимальную длину (хотя бы 50 символов)
        if (summary.length() < 50) {
            logger.warn("Сводка слишком короткая: {} символов", summary.length());
            return false;
        }
        
        // Проверяем, что сводка не является просто копией начала статьи
        String contentStart = originalContent.substring(0, Math.min(summary.length(), originalContent.length()));
        if (summary.equals(contentStart)) {
            logger.warn("Сводка является копией начала статьи");
            return false;
        }
        
        // Проверяем, что сводка содержит хотя бы одно предложение
        if (!summary.matches(".*[.!?].*")) {
            logger.warn("Сводка не содержит завершенных предложений");
            return false;
        }
        
        // Проверяем, что сводка не содержит технических артефактов
        if (summary.contains("```") || summary.contains("###") || summary.contains("---")) {
            logger.warn("Сводка содержит технические артефакты");
            return false;
        }
        
        return true;
    }

    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверка доступности LM Studio API
     */
    public boolean isApiAvailable() {
        if (!enabled) {
            return false;
        }
        
        try {
            webClient.get()
                    .uri(apiUrl + "/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            logger.warn("LM Studio API недоступен: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Проверка, настроен ли сервис
     */
    public boolean isConfigured() {
        return enabled && apiUrl != null && !apiUrl.isEmpty();
    }

    /**
     * Получение списка доступных моделей
     */
    public List<String> getAvailableModels() {
        if (!enabled) {
            return List.of();
        }
        
        try {
            String response = webClient.get()
                    .uri(apiUrl + "/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode data = jsonNode.path("data");
            
            if (data.isArray()) {
                return data.findValuesAsText("id");
            }
            
            return List.of();
        } catch (Exception e) {
            logger.error("Ошибка при получении списка моделей: {}", e.getMessage());
            return List.of();
        }
    }
}
