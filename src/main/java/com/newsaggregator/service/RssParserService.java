package com.newsaggregator.service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.ArticleStatus;
import com.newsaggregator.entity.Category;
import com.newsaggregator.entity.NewsSource;
import com.newsaggregator.repository.ArticleRepository;
import com.newsaggregator.repository.CategoryRepository;
import com.newsaggregator.repository.NewsSourceRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * Сервис для парсинга RSS лент и создания статей.
 * 
 * Основной сервис для автоматического получения новостей из RSS источников.
 * Предоставляет функциональность для:
 * - Парсинга RSS лент с использованием библиотеки Rome
 * - Извлечения полного контента статей с веб-страниц
 * - Извлечения изображений из RSS и веб-страниц
 * - ИИ-обработки статей (категоризация и генерация сводок)
 * - Обработки ошибок и управления источниками
 * - Очистки и нормализации текста статей
 * - Предотвращения дублирования статей
 * 
 * @author News Aggregator Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class RssParserService {

    private static final Logger logger = LoggerFactory.getLogger(RssParserService.class);

    @Autowired
    private NewsSourceRepository newsSourceRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LMStudioService lmStudioService;
    
    @Autowired
    private ArticleContentExtractorService contentExtractorService;
    
    @Value("${app.rss.max-articles-per-source:10}")
    private int maxArticlesPerSource;

    /**
     * Парсит все активные RSS источники
     */
    public void parseAllRssFeeds() {
        logger.info("Начинаем парсинг всех RSS лент...");
        
        List<NewsSource> activeSources = newsSourceRepository.findByActiveTrue();
        logger.info("Найдено {} активных источников", activeSources.size());

        for (NewsSource source : activeSources) {
            try {
                parseRssFeed(source);
            } catch (Exception e) {
                logger.error("Ошибка при парсинге источника {}: {}", source.getName(), e.getMessage());
                handleSourceError(source, e.getMessage());
            }
        }
        
        logger.info("Парсинг RSS лент завершен");
    }

    /**
     * Парсит конкретный RSS источник
     */
    public void parseRssFeed(NewsSource source) {
        logger.info("Парсинг RSS ленты: {} ({})", source.getName(), source.getRssUrl());

        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(source.getRssUrl())));

            int newArticlesCount = 0;
            int duplicatesCount = 0;
            int errorsCount = 0;
            int processedCount = 0;

            List<SyndEntry> entries = feed.getEntries();
            logger.info("Найдено {} статей в RSS ленте '{}', будет обработано максимум {}", 
                       entries.size(), source.getName(), maxArticlesPerSource);

            for (SyndEntry entry : entries) {
                // Ограничиваем количество обрабатываемых статей
                if (processedCount >= maxArticlesPerSource) {
                    logger.info("Достигнут лимит {} статей для источника '{}', остановка парсинга", 
                               maxArticlesPerSource, source.getName());
                    break;
                }
                
                try {
                    boolean result = processRssEntry(entry, source);
                    if (result) {
                        newArticlesCount++;
                    } else {
                        duplicatesCount++;
                    }
                    processedCount++;
                } catch (Exception e) {
                    errorsCount++;
                    processedCount++;
                    logger.warn("Ошибка при обработке статьи '{}': {}", 
                              entry.getTitle(), e.getMessage());
                }
            }

            // Обновляем информацию об источнике в отдельной транзакции
            updateSourceInfo(source, null, 0);

            logger.info("Источник '{}': обработано {}/{} статей, добавлено {} новых, {} дубликатов, {} ошибок", 
                       source.getName(), processedCount, entries.size(), newArticlesCount, duplicatesCount, errorsCount);

        } catch (Exception e) {
            logger.error("Ошибка при парсинге RSS ленты '{}': {}", source.getName(), e.getMessage());
            handleSourceError(source, e.getMessage());
        }
    }

    /**
     * Обрабатывает одну запись из RSS ленты
     */
    @Transactional
    private boolean processRssEntry(SyndEntry entry, NewsSource source) {
        try {
            String sourceUrl = entry.getLink();
            
            // Проверяем, не существует ли уже такая статья
            if (articleRepository.existsBySourceUrl(sourceUrl)) {
                return false; // Дубликат
            }

            // Создаем новую статью
            Article article = new Article();
            article.setTitle(cleanText(entry.getTitle()));
            article.setSourceUrl(sourceUrl);
            article.setSource(source);
            
            // Получаем краткое описание из RSS
            String rssDescription = "";
            if (entry.getDescription() != null) {
                rssDescription = cleanText(entry.getDescription().getValue());
            }
            
            // Пытаемся извлечь полный контент статьи
            String fullContent = null;
            if (contentExtractorService.shouldExtractContent(sourceUrl)) {
                try {
                    fullContent = contentExtractorService.extractFullContent(sourceUrl);
                    if (fullContent != null && !fullContent.trim().isEmpty()) {
                        logger.debug("Извлечен полный контент для статьи: '{}'", article.getTitle());
                    }
                } catch (Exception e) {
                    logger.warn("Не удалось извлечь полный контент для '{}': {}", article.getTitle(), e.getMessage());
                }
            }
            
            // Устанавливаем содержание (приоритет полному контенту)
            String content = fullContent != null && !fullContent.trim().isEmpty() ? fullContent : rssDescription;
            
            // Проверяем минимальную длину контента
            if (content == null || content.length() < 100) {
                logger.warn("Статья '{}' имеет слишком короткий контент ({}), пропускаем", 
                           article.getTitle(), content != null ? content.length() : 0);
                return false;
            }
            
            article.setContent(content);

            // Извлекаем изображение из RSS
            String imageUrl = extractImageFromEntry(entry);
            
            // Если изображение не найдено в RSS, пытаемся извлечь с веб-страницы
            if (imageUrl == null && contentExtractorService.shouldExtractContent(sourceUrl)) {
                try {
                    imageUrl = contentExtractorService.extractMainImage(sourceUrl);
                    if (imageUrl != null) {
                        logger.debug("Извлечено изображение с веб-страницы для статьи: '{}'", article.getTitle());
                    }
                } catch (Exception e) {
                    logger.debug("Не удалось извлечь изображение с веб-страницы для '{}': {}", article.getTitle(), e.getMessage());
                }
            }
            
            article.setImageUrl(imageUrl);

            // Устанавливаем дату публикации
            Date publishedDate = entry.getPublishedDate();
            if (publishedDate != null) {
                article.setPublishedAt(publishedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            } else {
                article.setPublishedAt(LocalDateTime.now());
            }

            // ИИ-обработка статьи (с обработкой ошибок)
            try {
                processArticleWithAI(article, content);
            } catch (Exception e) {
                logger.warn("Ошибка при ИИ-обработке статьи '{}': {}, используем базовую обработку", 
                           article.getTitle(), e.getMessage());
                processArticleBasic(article, content);
            }

            // Устанавливаем статус
            article.setStatus(ArticleStatus.PENDING);
            article.setCreatedAt(LocalDateTime.now());

            // Сохраняем статью
            articleRepository.save(article);
            
            logger.debug("Добавлена новая статья: '{}'", article.getTitle());
            return true;
            
        } catch (Exception e) {
            logger.error("Ошибка при обработке статьи '{}': {}", entry.getTitle(), e.getMessage());
            return false; // Возвращаем false вместо выброса исключения
        }
    }

    /**
     * Обрабатывает статью с помощью ИИ (LM Studio)
     */
    private void processArticleWithAI(Article article, String content) {
        try {
            // Проверяем доступность LM Studio API
            if (!lmStudioService.isConfigured()) {
                logger.warn("LM Studio API не настроен, используем базовую обработку");
                processArticleBasic(article, content);
                return;
            }

            // Генерируем сводку с помощью ИИ
            if (content != null && !content.isEmpty()) {
                String aiSummary = lmStudioService.generateSummary(content);
                article.setSummary(aiSummary);
                logger.debug("ИИ сгенерировал сводку для статьи: '{}'", article.getTitle());
            }

            // Определяем категорию с помощью ИИ
            String aiCategory = lmStudioService.categorizeArticle(article.getTitle(), content);
            Category category = findOrCreateCategory(aiCategory);
            article.setCategory(category);
            logger.debug("ИИ определил категорию '{}' для статьи: '{}'", aiCategory, article.getTitle());

        } catch (Exception e) {
            logger.error("Ошибка при ИИ-обработке статьи '{}': {}", article.getTitle(), e.getMessage());
            // Fallback на базовую обработку
            processArticleBasic(article, content);
        }
    }

    /**
     * Базовая обработка статьи без ИИ (fallback)
     */
    private void processArticleBasic(Article article, String content) {
        // Генерируем простую сводку
        article.setSummary(generateBasicSummary(content));
        
        // Определяем категорию по ключевым словам
        Category category = determineBasicCategory(article.getTitle(), content);
        article.setCategory(category);
    }

    /**
     * Находит или создает категорию
     */
    private Category findOrCreateCategory(String categoryName) {
        Optional<Category> existingCategory = categoryRepository.findByName(categoryName);
        
        if (existingCategory.isPresent()) {
            return existingCategory.get();
        }
        
        // Если категория не найдена, возвращаем категорию "Общее"
        return categoryRepository.findByName("Общество")
                .orElse(categoryRepository.findByName("Общее")
                        .orElse(null));
    }

    /**
     * Очищает текст от HTML тегов и лишних символов
     */
    private String cleanText(String text) {
        if (text == null) return null;
        
        // Удаляем HTML теги
        text = text.replaceAll("<[^>]+>", "");
        
        // Заменяем HTML entities
        text = text.replace("&nbsp;", " ")
                  .replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&quot;", "\"")
                  .replace("&#39;", "'");
        
        // Убираем навигационные элементы (хлебные крошки)
        text = text.replaceAll("Главная\\s*/\\s*[^/]+\\s*/\\s*\\d+\\s*(минут|час|день|недел|месяц)[^\\n]*назад\\s*", "");
        text = text.replaceAll("Главная\\s*/\\s*[^/]+\\s*/\\s*", "");
        
        // Убираем призывы к регистрации и сохранению
        text = text.replaceAll("Чтобы дочитать статью.*?зарегистрируйтесь\\.?", "");
        text = text.replaceAll("Чтобы продолжить чтение.*?зарегистрируйтесь\\.?", "");
        text = text.replaceAll("Для продолжения чтения.*?зарегистрируйтесь\\.?", "");
        text = text.replaceAll("сохраните\\s+[её]?e?\\s+в\\s+[«\"]?Отложенных материалах[»\"]?\\.?", "");
        text = text.replaceAll("Для этого войдите или зарегистрируйтесь\\.?", "");
        
        // Убираем типичные фразы из RSS
        text = text.replaceAll("Читать далее[\\s\\.]*", "");
        text = text.replaceAll("Read more[\\s\\.]*", "");
        text = text.replaceAll("Continue reading[\\s\\.]*", "");
        text = text.replaceAll("Подробнее[\\s\\.]*", "");
        
        // Убираем лишние пробелы
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }

    /**
     * Генерирует базовую краткую сводку статьи (делегирует в LMStudioService)
     */
    private String generateBasicSummary(String content) {
        // Используем метод из LMStudioService для единообразия
        return lmStudioService.generateSummary(content);
    }

    /**
     * Автоматически определяет категорию статьи по ключевым словам (базовый метод)
     */
    private Category determineBasicCategory(String title, String content) {
        String text = (title + " " + (content != null ? content : "")).toLowerCase();
        
        // Приоритет 1: Экономика и финансы (проверяем первой для финансовых новостей)
        if (containsKeywords(text, "экономик", "финанс", "банк", "рубль", "доллар", "инвестиц", "бизнес",
                            "акци", "биржа", "торг", "валют", "инфляц", "цб", "центробанк", "кредит",
                            "млрд", "млн", "фонд", "инвестфонд", "облигац", "бонд", "ценн", "капитал",
                            "прибыл", "убыт", "выручк", "дивиденд", "ипотек", "займ", "долг", "процент")) {
            return findCategoryByName("Экономика");
        }
        
        // Приоритет 2: Политика
        if (containsKeywords(text, "политик", "выбор", "правительств", "президент", "министр", "парламент", 
                            "медведев", "путин", "дума", "депутат", "закон", "санкц", "дипломат", 
                            "госдеп", "мид", "кремл", "белый дом", "конгресс", "сенат")) {
            return findCategoryByName("Политика");
        }
        
        // Приоритет 3: Технологии
        if (containsKeywords(text, "технолог", "программ", "компьютер", "софт", "it", "интернет", "цифров",
                            "код", "разработ", "api", "github", "python", "java", "javascript", "данных",
                            "искусственн", "нейрон", "машинн", "алгоритм", "сервер", "облак", "приложен",
                            "софт", "хард", "процессор", "чип", "смартфон", "гаджет")) {
            return findCategoryByName("Технологии");
        }
        
        // Спорт
        if (containsKeywords(text, "спорт", "футбол", "хоккей", "олимпиад", "чемпионат", "матч",
                            "команд", "игрок", "тренер", "турнир", "финал", "победа")) {
            return findCategoryByName("Спорт");
        }
        
        // Наука
        if (containsKeywords(text, "наук", "исследован", "открыт", "ученые", "эксперимент", "изучен",
                            "научн", "лаборатор", "университет", "академи")) {
            return findCategoryByName("Наука");
        }
        
        // Здоровье
        if (containsKeywords(text, "здоровь", "медицин", "врач", "лечен", "болезн", "вакцин",
                            "пациент", "клиник", "больниц", "терапи", "диагност")) {
            return findCategoryByName("Здоровье");
        }
        
        // По умолчанию - категория "Общество"
        return findCategoryByName("Общество");
    }

    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Category findCategoryByName(String name) {
        Optional<Category> category = categoryRepository.findByName(name);
        return category.orElse(null);
    }

    /**
     * Обновляет информацию об источнике в отдельной транзакции
     */
    @Transactional
    private void updateSourceInfo(NewsSource source, String errorMessage, int errorCount) {
        // Получаем свежую копию источника из БД
        NewsSource freshSource = newsSourceRepository.findById(source.getId()).orElse(source);
        
        freshSource.setLastUpdated(LocalDateTime.now());
        if (errorMessage != null) {
            freshSource.setLastError(errorMessage);
            freshSource.setErrorCount(errorCount);
        } else {
            freshSource.setLastError(null);
            freshSource.setErrorCount(0);
        }
        
        newsSourceRepository.save(freshSource);
    }

    /**
     * Обрабатывает ошибку источника
     */
    @Transactional
    private void handleSourceError(NewsSource source, String errorMessage) {
        // Получаем свежую копию источника из БД
        NewsSource freshSource = newsSourceRepository.findById(source.getId()).orElse(source);
        
        freshSource.setLastError(errorMessage);
        freshSource.setErrorCount(freshSource.getErrorCount() + 1);
        
        // Отключаем источник после 5 ошибок подряд
        if (freshSource.getErrorCount() >= 5) {
            freshSource.setActive(false);
            logger.warn("Источник '{}' отключен после {} ошибок подряд", 
                       freshSource.getName(), freshSource.getErrorCount());
        }
        
        newsSourceRepository.save(freshSource);
    }
    
    /**
     * Очищает summary всех статей от навигации и призывов к регистрации
     */
    @Transactional
    public int cleanAllArticleSummaries() {
        logger.info("Начинаем очистку summary всех статей...");
        
        List<Article> articles = articleRepository.findAll();
        int cleanedCount = 0;
        
        for (Article article : articles) {
            if (article.getSummary() != null && !article.getSummary().isEmpty()) {
                String originalSummary = article.getSummary();
                String cleanedSummary = cleanSummaryText(originalSummary);
                
                if (!originalSummary.equals(cleanedSummary)) {
                    article.setSummary(cleanedSummary);
                    articleRepository.save(article);
                    cleanedCount++;
                    logger.debug("Очищен summary для статьи: '{}'", article.getTitle());
                }
            }
        }
        
        logger.info("Очистка завершена. Обработано {} статей", cleanedCount);
        return cleanedCount;
    }
    
    /**
     * Очищает текст summary от навигации и мусора
     */
    private String cleanSummaryText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Убираем навигационные элементы (хлебные крошки)
        text = text.replaceAll("Главная\\s*/\\s*[^/]+\\s*/\\s*\\d+\\s*(минут|час|день|недел|месяц)[^\\n]*назад\\s*", "");
        text = text.replaceAll("Главная\\s*/\\s*[^/]+\\s*/\\s*", "");
        
        // Убираем призывы к регистрации и сохранению
        text = text.replaceAll("Чтобы дочитать статью.*?зарегистрируйтесь\\.?", "");
        text = text.replaceAll("Чтобы продолжить чтение.*?зарегистрируйтесь\\.?", "");
        text = text.replaceAll("Для продолжения чтения.*?зарегистрируйтесь\\.?", "");
        text = text.replaceAll("сохраните\\s+[её]?e?\\s+в\\s+[«\"]?Отложенных материалах[»\"]?\\.?", "");
        text = text.replaceAll("Для этого войдите или зарегистрируйтесь\\.?", "");
        
        // Убираем название источника в начале (например "Ведомости")
        text = text.replaceAll("^[А-ЯЁ][а-яё]+\\s+", "");
        
        // Убираем лишние пробелы
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }
    
    /**
     * Извлекает URL изображения из RSS записи
     */
    private String extractImageFromEntry(SyndEntry entry) {
        try {
            // 1. Проверяем enclosures (вложения)
            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                for (com.rometools.rome.feed.synd.SyndEnclosure enclosure : entry.getEnclosures()) {
                    String type = enclosure.getType();
                    if (type != null && type.startsWith("image/")) {
                        String url = enclosure.getUrl();
                        if (isValidImageUrl(url)) {
                            logger.debug("Найдено изображение в enclosure: {}", url);
                            return url;
                        }
                    }
                }
            }
            
            // 2. Проверяем содержимое description на наличие img тегов
            if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
                String imageUrl = extractImageFromHtml(entry.getDescription().getValue());
                if (imageUrl != null) {
                    logger.debug("Найдено изображение в description: {}", imageUrl);
                    return imageUrl;
                }
            }
            
            // 3. Проверяем contents
            if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                for (com.rometools.rome.feed.synd.SyndContent content : entry.getContents()) {
                    if (content.getValue() != null) {
                        String imageUrl = extractImageFromHtml(content.getValue());
                        if (imageUrl != null) {
                            logger.debug("Найдено изображение в content: {}", imageUrl);
                            return imageUrl;
                        }
                    }
                }
            }
            
            // 4. Проверяем foreign markup (для Media RSS и других расширений)
            if (entry.getForeignMarkup() != null && !entry.getForeignMarkup().isEmpty()) {
                for (org.jdom2.Element element : entry.getForeignMarkup()) {
                    String imageUrl = extractImageFromElement(element);
                    if (imageUrl != null) {
                        logger.debug("Найдено изображение в foreign markup: {}", imageUrl);
                        return imageUrl;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("Ошибка при извлечении изображения из RSS записи '{}': {}", 
                       entry.getTitle(), e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Извлекает URL изображения из HTML контента
     */
    private String extractImageFromHtml(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        // Ищем первый img тег
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = imgPattern.matcher(html);
        if (matcher.find()) {
            String imageUrl = matcher.group(1);
            if (isValidImageUrl(imageUrl)) {
                return imageUrl;
            }
        }
        
        return null;
    }
    
    /**
     * Извлекает URL изображения из XML элемента (Media RSS и другие)
     */
    private String extractImageFromElement(org.jdom2.Element element) {
        try {
            // Media RSS: <media:content url="..." type="image/..."/>
            if ("content".equals(element.getName()) && 
                "http://search.yahoo.com/mrss/".equals(element.getNamespaceURI())) {
                String type = element.getAttributeValue("type");
                if (type != null && type.startsWith("image/")) {
                    String url = element.getAttributeValue("url");
                    if (isValidImageUrl(url)) {
                        return url;
                    }
                }
            }
            
            // Media RSS: <media:thumbnail url="..."/>
            if ("thumbnail".equals(element.getName()) && 
                "http://search.yahoo.com/mrss/".equals(element.getNamespaceURI())) {
                String url = element.getAttributeValue("url");
                if (isValidImageUrl(url)) {
                    return url;
                }
            }
            
            // Рекурсивно проверяем дочерние элементы
            for (org.jdom2.Element child : element.getChildren()) {
                String imageUrl = extractImageFromElement(child);
                if (imageUrl != null) {
                    return imageUrl;
                }
            }
            
        } catch (Exception e) {
            logger.debug("Ошибка при обработке XML элемента: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Проверяет, является ли URL валидным URL изображения
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Убираем пробелы
        url = url.trim();
        
        // Проверяем, что это HTTP/HTTPS URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        
        // Проверяем расширение файла
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("?")) {
            // Убираем параметры запроса для проверки расширения
            lowerUrl = lowerUrl.substring(0, lowerUrl.indexOf("?"));
        }
        
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || 
               lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") || 
               lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".svg") ||
               // Также принимаем URL без расширения (могут быть динамические изображения)
               (!lowerUrl.contains(".") && url.length() > 20);
    }
}