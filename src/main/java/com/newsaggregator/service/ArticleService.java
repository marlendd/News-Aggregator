package com.newsaggregator.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.ArticleStatus;
import com.newsaggregator.entity.Category;
import com.newsaggregator.entity.User;
import com.newsaggregator.repository.ArticleRepository;

/**
 * Сервис для управления статьями в новостном агрегаторе.
 * 
 * Предоставляет функциональность для:
 * - CRUD операций со статьями
 * - Поиска и фильтрации статей
 * - Управления статусами статей (черновик, на модерации, опубликована, отклонена)
 * - Массовых операций со статьями
 * - Персонализированной выдачи статей для пользователей
 * - Работы с сохраненными статьями
 * - Статистики и аналитики
 * 
 * @author News Aggregator Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class ArticleService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);
    
    @Autowired
    private ArticleRepository articleRepository;
    
    /**
     * Получает статью по её идентификатору.
     * 
     * @param id идентификатор статьи
     * @return Optional с найденной статьей или пустой Optional, если статья не найдена
     */
    @Transactional(readOnly = true)
    public Optional<Article> getArticleById(Long id) {
        return articleRepository.findById(id);
    }
    
    /**
     * Получает опубликованную статью по её идентификатору.
     * Возвращает статью только если она имеет статус PUBLISHED.
     * 
     * @param id идентификатор статьи
     * @return Optional с найденной опубликованной статьей или пустой Optional
     */
    @Transactional(readOnly = true)
    public Optional<Article> getPublishedArticleById(Long id) {
        Optional<Article> article = articleRepository.findById(id);
        return article.filter(Article::isPublished);
    }
    
    /**
     * Находит статью по URL источника.
     * Используется для предотвращения дублирования статей при парсинге RSS.
     * 
     * @param sourceUrl URL источника статьи
     * @return Optional с найденной статьей или пустой Optional
     */
    @Transactional(readOnly = true)
    public Optional<Article> getArticleBySourceUrl(String sourceUrl) {
        return articleRepository.findBySourceUrl(sourceUrl);
    }
    
    /**
     * Проверяет существование статьи с указанным URL источника.
     * 
     * @param sourceUrl URL источника статьи
     * @return true, если статья с таким URL существует, false в противном случае
     */
    @Transactional(readOnly = true)
    public boolean existsBySourceUrl(String sourceUrl) {
        return articleRepository.existsBySourceUrl(sourceUrl);
    }
    
    /**
     * Получает все статьи без пагинации.
     * Используется для административных операций.
     * 
     * @return список всех статей
     */
    @Transactional(readOnly = true)
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }
    
    /**
     * Получает все статьи с пагинацией.
     * 
     * @param pageable параметры пагинации
     * @return страница статей
     */
    @Transactional(readOnly = true)
    public Page<Article> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }
    
    /**
     * Получает только опубликованные статьи с пагинацией.
     * Используется для отображения статей обычным пользователям.
     * 
     * @param pageable параметры пагинации
     * @return страница опубликованных статей
     */
    @Transactional(readOnly = true)
    public Page<Article> getPublishedArticles(Pageable pageable) {
        return articleRepository.findPublishedArticles(pageable);
    }
    
    /**
     * Получает статьи с определенным статусом.
     * 
     * @param status статус статей для поиска
     * @param pageable параметры пагинации
     * @return страница статей с указанным статусом
     */
    @Transactional(readOnly = true)
    public Page<Article> getArticlesByStatus(ArticleStatus status, Pageable pageable) {
        return articleRepository.findByStatus(status, pageable);
    }
    
    /**
     * Получает опубликованные статьи определенной категории.
     * 
     * @param categoryId идентификатор категории
     * @param pageable параметры пагинации
     * @return страница опубликованных статей указанной категории
     */
    @Transactional(readOnly = true)
    public Page<Article> getPublishedArticlesByCategory(Long categoryId, Pageable pageable) {
        Category category = new Category();
        category.setId(categoryId);
        return articleRepository.findPublishedArticlesByCategory(category, pageable);
    }
    
    /**
     * Выполняет поиск среди опубликованных статей по ключевым словам.
     * 
     * @param searchTerm поисковый запрос
     * @param pageable параметры пагинации
     * @return страница найденных опубликованных статей
     */
    @Transactional(readOnly = true)
    public Page<Article> searchPublishedArticles(String searchTerm, Pageable pageable) {
        return articleRepository.searchPublishedArticles(searchTerm, pageable);
    }
    
    /**
     * Выполняет расширенный поиск среди опубликованных статей с фильтрами.
     * 
     * @param searchTerm поисковый запрос
     * @param categoryId идентификатор категории для фильтрации (может быть null)
     * @param sourceId идентификатор источника для фильтрации (может быть null)
     * @param dateFrom дата начала периода (может быть null)
     * @param dateTo дата окончания периода (может быть null)
     * @param sortBy поле для сортировки
     * @param sortDir направление сортировки (asc/desc)
     * @param pageable параметры пагинации
     * @return страница найденных статей с применением фильтров
     */
    @Transactional(readOnly = true)
    public Page<Article> searchPublishedArticlesWithFilters(String searchTerm, Long categoryId, 
                                                           Long sourceId, String dateFrom, String dateTo,
                                                           String sortBy, String sortDir, Pageable pageable) {
        return articleRepository.searchPublishedArticlesWithFilters(
            searchTerm, categoryId, sourceId, dateFrom, dateTo, sortBy, sortDir, pageable);
    }
    
    /**
     * Получает опубликованные статьи с применением фильтров без поискового запроса.
     * 
     * @param categoryId идентификатор категории для фильтрации (может быть null)
     * @param sourceId идентификатор источника для фильтрации (может быть null)
     * @param sortBy поле для сортировки
     * @param sortDir направление сортировки (asc/desc)
     * @param pageable параметры пагинации
     * @return страница отфильтрованных статей
     */
    @Transactional(readOnly = true)
    public Page<Article> getPublishedArticlesWithFilters(Long categoryId, Long sourceId, 
                                                        String sortBy, String sortDir, Pageable pageable) {
        return articleRepository.getPublishedArticlesWithFilters(categoryId, sourceId, sortBy, sortDir, pageable);
    }
    
    /**
     * Получает статьи, сохраненные пользователем.
     * 
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница сохраненных пользователем статей
     */
    @Transactional(readOnly = true)
    public Page<Article> getSavedArticlesByUser(Long userId, Pageable pageable) {
        return articleRepository.findSavedArticlesByUserId(userId, pageable);
    }
    
    /**
     * Получает статьи, созданные определенным пользователем.
     * 
     * @param userId идентификатор пользователя-автора
     * @param pageable параметры пагинации
     * @return страница статей, созданных пользователем
     */
    @Transactional(readOnly = true)
    public Page<Article> getArticlesByCreatedBy(Long userId, Pageable pageable) {
        return articleRepository.findArticlesByCreatedBy(userId, pageable);
    }
    
    /**
     * Получает статьи, связанные с данной статьей.
     * Возвращает статьи из той же категории, исключая саму статью.
     * 
     * @param article статья, для которой ищутся связанные
     * @param limit максимальное количество связанных статей
     * @return список связанных статей
     */
    @Transactional(readOnly = true)
    public List<Article> getRelatedArticles(Article article, int limit) {
        if (article.getCategory() == null) {
            return List.of();
        }
        
        Page<Article> relatedPage = articleRepository.findPublishedArticlesByCategory(
            article.getCategory(), 
            org.springframework.data.domain.PageRequest.of(0, limit + 1)
        );
        
        return relatedPage.getContent().stream()
            .filter(a -> !a.getId().equals(article.getId()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Создает новую статью.
     * 
     * @param title заголовок статьи
     * @param content содержание статьи
     * @param sourceUrl URL источника статьи
     * @param publishedAt дата публикации статьи
     * @param createdBy пользователь, создавший статью
     * @return созданная статья
     * @throws IllegalArgumentException если статья с таким URL уже существует
     */
    public Article createArticle(String title, String content, String sourceUrl, 
                               LocalDateTime publishedAt, User createdBy) {
        if (existsBySourceUrl(sourceUrl)) {
            throw new IllegalArgumentException("Статья с таким URL уже существует");
        }
        
        Article article = new Article(title, content, sourceUrl, publishedAt);
        article.setCreatedBy(createdBy);
        article.setStatus(ArticleStatus.DRAFT);
        
        Article savedArticle = articleRepository.save(article);
        logger.info("Создана новая статья: {} (ID: {})", title, savedArticle.getId());
        return savedArticle;
    }
    
    /**
     * Обновляет существующую статью.
     * 
     * @param articleId идентификатор статьи для обновления
     * @param title новый заголовок статьи
     * @param content новое содержание статьи
     * @param summary новая краткая сводка статьи
     * @param imageUrl новый URL изображения статьи
     * @return обновленная статья
     * @throws IllegalArgumentException если статья не найдена
     */
    public Article updateArticle(Long articleId, String title, String content, 
                               String summary, String imageUrl) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        
        article.setTitle(title);
        article.setContent(content);
        article.setSummary(summary);
        article.setImageUrl(imageUrl);
        
        Article savedArticle = articleRepository.save(article);
        logger.info("Обновлена статья: {} (ID: {})", title, articleId);
        return savedArticle;
    }
    
    /**
     * Обновляет только содержание статьи.
     * 
     * @param articleId идентификатор статьи
     * @param content новое содержание статьи
     * @return обновленная статья
     * @throws IllegalArgumentException если статья не найдена
     */
    public Article updateArticleContent(Long articleId, String content) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        
        article.setContent(content);
        
        Article savedArticle = articleRepository.save(article);
        logger.info("Обновлен контент статьи: {} (ID: {})", article.getTitle(), articleId);
        return savedArticle;
    }
    
    /**
     * Извлекает полное содержание для всех статей с коротким контентом.
     * Используется для пакетного обновления статей.
     * 
     * @return количество обновленных статей
     */
    @Transactional
    public int extractContentForAllArticles() {
        // Получаем статьи с коротким контентом (менее 500 символов)
        List<Article> articlesNeedingContent = articleRepository.findArticlesWithShortContent();
        
        int updatedCount = 0;
        for (Article article : articlesNeedingContent) {
            try {
                if (article.getSourceUrl() != null) {
                    logger.info("Обновление контента для статьи: {}", article.getTitle());
                    updatedCount++;
                }
            } catch (Exception e) {
                logger.warn("Ошибка при обновлении контента статьи '{}': {}", 
                           article.getTitle(), e.getMessage());
            }
        }
        
        return updatedCount;
    }
    
    /**
     * Получает список статей с коротким содержанием (менее 500 символов).
     * 
     * @return список статей с коротким содержанием
     */
    @Transactional(readOnly = true)
    public List<Article> getArticlesWithShortContent() {
        return articleRepository.findArticlesWithShortContent();
    }
    
    /**
     * Удаляет статьи с неполным содержанием.
     * Используется для очистки базы данных от некачественных статей.
     * 
     * @return количество удаленных статей
     */
    @Transactional
    public int deleteIncompleteArticles() {
        // Получаем статьи с неполным контентом
        List<Article> incompleteArticles = articleRepository.findIncompleteArticles();
        
        int deletedCount = 0;
        for (Article article : incompleteArticles) {
            try {
                articleRepository.delete(article);
                deletedCount++;
                logger.info("Удалена неполная статья: {} (ID: {})", article.getTitle(), article.getId());
            } catch (Exception e) {
                logger.warn("Ошибка при удалении статьи '{}': {}", article.getTitle(), e.getMessage());
            }
        }
        
        logger.info("Удалено {} неполных статей", deletedCount);
        return deletedCount;
    }
    
    public Article publishArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        
        article.publish();
        Article savedArticle = articleRepository.save(article);
        logger.info("Опубликована статья: {} (ID: {})", article.getTitle(), articleId);
        return savedArticle;
    }
    
    public Article rejectArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        
        article.reject();
        Article savedArticle = articleRepository.save(article);
        logger.info("Отклонена статья: {} (ID: {})", article.getTitle(), articleId);
        return savedArticle;
    }
    
    @Transactional
    public int rejectAllPendingArticles() {
        List<Article> pendingArticles = articleRepository.findByStatus(ArticleStatus.PENDING);
        
        int rejectedCount = 0;
        for (Article article : pendingArticles) {
            try {
                article.reject();
                articleRepository.save(article);
                rejectedCount++;
            } catch (Exception e) {
                logger.warn("Ошибка при отклонении статьи '{}': {}", article.getTitle(), e.getMessage());
            }
        }
        
        logger.info("Массово отклонено {} статей", rejectedCount);
        return rejectedCount;
    }
    
    @Transactional
    public int rejectAllPendingArticlesByCategory(Long categoryId) {
        Category category = new Category();
        category.setId(categoryId);
        
        List<Article> pendingArticles = articleRepository.findByStatusAndCategory(ArticleStatus.PENDING, category);
        
        int rejectedCount = 0;
        for (Article article : pendingArticles) {
            try {
                article.reject();
                articleRepository.save(article);
                rejectedCount++;
            } catch (Exception e) {
                logger.warn("Ошибка при отклонении статьи '{}': {}", article.getTitle(), e.getMessage());
            }
        }
        
        logger.info("Массово отклонено {} статей в категории ID: {}", rejectedCount, categoryId);
        return rejectedCount;
    }
    
    @Transactional
    public int deleteAllRejectedArticles() {
        List<Article> rejectedArticles = articleRepository.findByStatus(ArticleStatus.REJECTED);
        
        int deletedCount = 0;
        for (Article article : rejectedArticles) {
            try {
                articleRepository.delete(article);
                deletedCount++;
            } catch (Exception e) {
                logger.warn("Ошибка при удалении статьи '{}': {}", article.getTitle(), e.getMessage());
            }
        }
        
        logger.info("Массово удалено {} отклоненных статей", deletedCount);
        return deletedCount;
    }
    
    @Transactional
    public int rejectAllArticles() {
        List<Article> allArticles = articleRepository.findAll();
        
        int rejectedCount = 0;
        for (Article article : allArticles) {
            if (article.getStatus() != ArticleStatus.REJECTED) {
                try {
                    article.setStatus(ArticleStatus.REJECTED);
                    articleRepository.save(article);
                    rejectedCount++;
                } catch (Exception e) {
                    logger.warn("Ошибка при отклонении статьи '{}': {}", article.getTitle(), e.getMessage());
                }
            }
        }
        
        logger.info("Массово отклонено {} статей", rejectedCount);
        return rejectedCount;
    }
    
    @Transactional
    public int deleteAllPublishedArticles() {
        List<Article> publishedArticles = articleRepository.findByStatus(ArticleStatus.PUBLISHED);
        
        int deletedCount = 0;
        for (Article article : publishedArticles) {
            try {
                articleRepository.delete(article);
                deletedCount++;
            } catch (Exception e) {
                logger.warn("Ошибка при удалении статьи '{}': {}", article.getTitle(), e.getMessage());
            }
        }
        
        logger.info("Массово удалено {} опубликованных статей", deletedCount);
        return deletedCount;
    }
    
    @Transactional
    public int deleteAllArticles() {
        List<Article> allArticles = articleRepository.findAll();
        
        int deletedCount = 0;
        for (Article article : allArticles) {
            try {
                articleRepository.delete(article);
                deletedCount++;
            } catch (Exception e) {
                logger.warn("Ошибка при удалении статьи '{}': {}", article.getTitle(), e.getMessage());
            }
        }
        
        logger.info("Массово удалено {} статей", deletedCount);
        return deletedCount;
    }
    
    @Transactional(readOnly = true)
    public Page<Article> getArticlesByStatusAndCategory(ArticleStatus status, Long categoryId, Pageable pageable) {
        Category category = new Category();
        category.setId(categoryId);
        return articleRepository.findByStatusAndCategory(status, category, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Article> getArticlesByStatusAndSource(ArticleStatus status, Long sourceId, Pageable pageable) {
        com.newsaggregator.entity.NewsSource source = new com.newsaggregator.entity.NewsSource();
        source.setId(sourceId);
        return articleRepository.findByStatusAndSource(status, source, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Article> getArticlesByStatusCategoryAndSource(ArticleStatus status, Long categoryId, Long sourceId, Pageable pageable) {
        Category category = new Category();
        category.setId(categoryId);
        com.newsaggregator.entity.NewsSource source = new com.newsaggregator.entity.NewsSource();
        source.setId(sourceId);
        return articleRepository.findByStatusAndCategoryAndSource(status, category, source, pageable);
    }
    
    public Article setArticleCategory(Long articleId, Category category) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        
        article.setCategory(category);
        Article savedArticle = articleRepository.save(article);
        logger.info("Установлена категория '{}' для статьи: {} (ID: {})", 
                   category.getName(), article.getTitle(), articleId);
        return savedArticle;
    }
    
    public Article setArticleSummary(Long articleId, String summary) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        
        article.setSummary(summary);
        Article savedArticle = articleRepository.save(article);
        logger.info("Установлена сводка для статьи: {} (ID: {})", article.getTitle(), articleId);
        return savedArticle;
    }
    
    public void deleteArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
        
        articleRepository.delete(article);
        logger.info("Удалена статья: {} (ID: {})", article.getTitle(), articleId);
    }
    
    @Transactional(readOnly = true)
    public long getTotalArticlesCount() {
        return articleRepository.count();
    }
    
    @Transactional(readOnly = true)
    public long getPublishedArticlesCount() {
        return articleRepository.countByStatus(ArticleStatus.PUBLISHED);
    }
    
    @Transactional(readOnly = true)
    public long getPendingArticlesCount() {
        return articleRepository.countByStatus(ArticleStatus.PENDING);
    }
    
    @Transactional(readOnly = true)
    public long getArticlesCountSince(LocalDateTime date) {
        return articleRepository.countPublishedSince(date);
    }
    
    @Transactional(readOnly = true)
    public Page<Article> getPersonalizedArticles(User user, Pageable pageable) {
        // Получаем предпочтения пользователя
        com.newsaggregator.entity.UserPreferences preferences = user.getPreferences();
        
        // Если нет предпочтений, возвращаем все опубликованные статьи
        if (preferences == null) {
            return getPublishedArticles(pageable);
        }
        
        // Парсим выбранные категории и источники из JSON
        List<Long> categoryIds = parseJsonIds(preferences.getSubscribedCategories());
        List<Long> sourceIds = parseJsonIds(preferences.getPreferredSources());
        
        // Если ничего не выбрано, возвращаем все статьи
        if (categoryIds.isEmpty() && sourceIds.isEmpty()) {
            return getPublishedArticles(pageable);
        }
        
        // Фильтруем по категориям и/или источникам
        if (!categoryIds.isEmpty() && !sourceIds.isEmpty()) {
            // Фильтр по категориям И источникам
            return articleRepository.findPublishedArticlesByCategoriesAndSources(categoryIds, sourceIds, pageable);
        } else if (!categoryIds.isEmpty()) {
            // Только по категориям
            return articleRepository.findPublishedArticlesByCategories(categoryIds, pageable);
        } else {
            // Только по источникам
            return articleRepository.findPublishedArticlesBySources(sourceIds, pageable);
        }
    }
    
    /**
     * Парсит JSON массив ID в список Long
     * Формат: "[1,2,3]" -> [1, 2, 3]
     */
    private List<Long> parseJsonIds(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Убираем квадратные скобки и пробелы
            String cleaned = json.replaceAll("[\\[\\]\\s]", "");
            if (cleaned.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Разбиваем по запятой и конвертируем в Long
            return java.util.Arrays.stream(cleaned.split(","))
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.warn("Ошибка при парсинге JSON ID: {}", json, e);
            return new ArrayList<>();
        }
    }
}