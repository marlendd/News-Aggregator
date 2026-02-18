package com.newsaggregator.functional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.ArticleStatus;
import com.newsaggregator.entity.Category;
import com.newsaggregator.entity.NewsSource;
import com.newsaggregator.repository.ArticleRepository;
import com.newsaggregator.repository.CategoryRepository;
import com.newsaggregator.repository.NewsSourceRepository;
import com.newsaggregator.service.ArticleService;

/**
 * Функциональные тесты для полного жизненного цикла статьи
 * Проверяют сценарии от создания до публикации/удаления
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Article Workflow Functional Tests")
class ArticleWorkflowTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private NewsSourceRepository newsSourceRepository;

    private Category testCategory;
    private NewsSource testSource;

    @BeforeEach
    void setUp() {
        // Создаем тестовую категорию
        testCategory = new Category();
        testCategory.setName("Тестовая категория");
        testCategory.setDescription("Описание");
        testCategory.setColorCode("#007bff");
        testCategory = categoryRepository.save(testCategory);

        // Создаем тестовый источник
        testSource = new NewsSource();
        testSource.setName("Тестовый источник");
        testSource.setRssUrl("http://test.com/rss");
        testSource.setActive(true);
        testSource = newsSourceRepository.save(testSource);
    }

    @Test
    @DisplayName("Полный жизненный цикл статьи: создание -> модерация -> публикация")
    void testCompleteArticleLifecycle() {
        // 1. Создание статьи
        Article article = new Article();
        article.setTitle("Тестовая статья");
        article.setSummary("Краткое описание");
        article.setContent("Полный текст статьи с подробным описанием события");
        article.setSourceUrl("http://test.com/article1");
        article.setStatus(ArticleStatus.PENDING);
        article.setCategory(testCategory);
        article.setSource(testSource);
        article.setCreatedAt(LocalDateTime.now());
        article.setPublishedAt(LocalDateTime.now());

        Article savedArticle = articleRepository.save(article);
        assertNotNull(savedArticle.getId());
        assertEquals(ArticleStatus.PENDING, savedArticle.getStatus());

        // 2. Проверка, что статья на модерации
        Page<Article> pendingArticles = articleService.getArticlesByStatus(
                ArticleStatus.PENDING, PageRequest.of(0, 10));
        assertTrue(pendingArticles.getTotalElements() > 0);

        // 3. Публикация статьи
        articleService.publishArticle(savedArticle.getId());

        // 4. Проверка, что статья опубликована
        Optional<Article> publishedArticle = articleService.getArticleById(savedArticle.getId());
        assertTrue(publishedArticle.isPresent());
        assertEquals(ArticleStatus.PUBLISHED, publishedArticle.get().getStatus());
        assertNotNull(publishedArticle.get().getPublishedAt());

        // 5. Проверка, что статья появилась в списке опубликованных
        Page<Article> publishedArticles = articleService.getPublishedArticles(PageRequest.of(0, 10));
        assertTrue(publishedArticles.getTotalElements() > 0);
    }

    @Test
    @DisplayName("Сценарий отклонения статьи")
    void testArticleRejectionWorkflow() {
        // 1. Создание статьи
        Article article = new Article();
        article.setTitle("Статья для отклонения");
        article.setSummary("Описание");
        article.setContent("Контент статьи");
        article.setSourceUrl("http://test.com/article2");
        article.setStatus(ArticleStatus.PENDING);
        article.setCategory(testCategory);
        article.setSource(testSource);
        article.setCreatedAt(LocalDateTime.now());
        article.setPublishedAt(LocalDateTime.now());

        Article savedArticle = articleRepository.save(article);

        // 2. Отклонение статьи
        articleService.rejectArticle(savedArticle.getId());

        // 3. Проверка статуса
        Optional<Article> rejectedArticle = articleService.getArticleById(savedArticle.getId());
        assertTrue(rejectedArticle.isPresent());
        assertEquals(ArticleStatus.REJECTED, rejectedArticle.get().getStatus());

        // 4. Проверка, что статья не в списке опубликованных
        Page<Article> publishedArticles = articleService.getPublishedArticles(PageRequest.of(0, 10));
        assertFalse(publishedArticles.getContent().contains(rejectedArticle.get()));
    }

    @Test
    @DisplayName("Сценарий обновления контента статьи")
    void testArticleContentUpdateWorkflow() {
        // 1. Создание и публикация статьи
        Article article = new Article();
        article.setTitle("Статья для обновления");
        article.setSummary("Описание");
        article.setContent("Исходный контент");
        article.setSourceUrl("http://test.com/article3");
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setCategory(testCategory);
        article.setSource(testSource);
        article.setCreatedAt(LocalDateTime.now());
        article.setPublishedAt(LocalDateTime.now());

        Article savedArticle = articleRepository.save(article);

        // 2. Обновление контента
        String newContent = "Обновленный контент с дополнительной информацией";
        articleService.updateArticleContent(savedArticle.getId(), newContent);

        // 3. Проверка обновления
        Optional<Article> updatedArticle = articleService.getArticleById(savedArticle.getId());
        assertTrue(updatedArticle.isPresent());
        assertEquals(newContent, updatedArticle.get().getContent());
    }

    @Test
    @DisplayName("Сценарий удаления статьи")
    void testArticleDeletionWorkflow() {
        // 1. Создание статьи
        Article article = new Article();
        article.setTitle("Статья для удаления");
        article.setSummary("Описание");
        article.setContent("Контент");
        article.setSourceUrl("http://test.com/article4");
        article.setStatus(ArticleStatus.PENDING);
        article.setCategory(testCategory);
        article.setSource(testSource);
        article.setCreatedAt(LocalDateTime.now());
        article.setPublishedAt(LocalDateTime.now());

        Article savedArticle = articleRepository.save(article);
        Long articleId = savedArticle.getId();

        // 2. Удаление статьи
        articleService.deleteArticle(articleId);

        // 3. Проверка, что статья удалена
        Optional<Article> deletedArticle = articleService.getArticleById(articleId);
        assertFalse(deletedArticle.isPresent());
    }

    @Test
    @DisplayName("Сценарий фильтрации статей по категории")
    void testArticleFilteringByCategoryWorkflow() {
        // 1. Создание нескольких статей в одной категории
        for (int i = 1; i <= 3; i++) {
            Article article = new Article();
            article.setTitle("Статья " + i);
            article.setSummary("Описание " + i);
            article.setContent("Контент " + i);
            article.setSourceUrl("http://test.com/article" + i);
            article.setStatus(ArticleStatus.PUBLISHED);
            article.setCategory(testCategory);
            article.setSource(testSource);
            article.setCreatedAt(LocalDateTime.now());
            article.setPublishedAt(LocalDateTime.now());
            articleRepository.save(article);
        }

        // 2. Получение статей по категории
        Page<Article> categoryArticles = articleRepository.findByStatusAndCategory(
                ArticleStatus.PUBLISHED, testCategory, PageRequest.of(0, 10));

        // 3. Проверка результатов
        assertTrue(categoryArticles.getTotalElements() >= 3);
        categoryArticles.getContent().forEach(article -> 
            assertEquals(testCategory.getId(), article.getCategory().getId())
        );
    }

    @Test
    @DisplayName("Сценарий подсчета статей по статусам")
    void testArticleCountingWorkflow() {
        // 1. Создание статей с разными статусами
        createArticleWithStatus("Pending 1", ArticleStatus.PENDING);
        createArticleWithStatus("Pending 2", ArticleStatus.PENDING);
        createArticleWithStatus("Published 1", ArticleStatus.PUBLISHED);
        createArticleWithStatus("Rejected 1", ArticleStatus.REJECTED);

        // 2. Подсчет статей
        long totalArticles = articleService.getTotalArticlesCount();
        long publishedCount = articleService.getPublishedArticlesCount();
        long pendingCount = articleService.getPendingArticlesCount();

        // 3. Проверка результатов
        assertTrue(totalArticles >= 4);
        assertTrue(publishedCount >= 1);
        assertTrue(pendingCount >= 2);
    }

    @Test
    @DisplayName("Сценарий проверки уникальности URL статьи")
    void testArticleUrlUniquenessWorkflow() {
        // 1. Создание статьи с уникальным URL
        String uniqueUrl = "http://test.com/unique-article-" + System.currentTimeMillis();
        Article article1 = new Article();
        article1.setTitle("Первая статья");
        article1.setSummary("Описание");
        article1.setContent("Контент");
        article1.setSourceUrl(uniqueUrl);
        article1.setStatus(ArticleStatus.PENDING);
        article1.setCategory(testCategory);
        article1.setSource(testSource);
        article1.setCreatedAt(LocalDateTime.now());
        article1.setPublishedAt(LocalDateTime.now());

        articleRepository.save(article1);

        // 2. Проверка существования статьи по URL
        boolean exists = articleRepository.existsBySourceUrl(uniqueUrl);
        assertTrue(exists);

        // 3. Поиск статьи по URL
        Optional<Article> foundArticle = articleRepository.findBySourceUrl(uniqueUrl);
        assertTrue(foundArticle.isPresent());
        assertEquals("Первая статья", foundArticle.get().getTitle());
    }

    private void createArticleWithStatus(String title, ArticleStatus status) {
        Article article = new Article();
        article.setTitle(title);
        article.setSummary("Описание");
        article.setContent("Контент");
        article.setSourceUrl("http://test.com/" + title.replace(" ", "-"));
        article.setStatus(status);
        article.setCategory(testCategory);
        article.setSource(testSource);
        article.setCreatedAt(LocalDateTime.now());
        article.setPublishedAt(LocalDateTime.now());
        
        articleRepository.save(article);
    }
}
