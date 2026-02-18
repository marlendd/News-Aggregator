package com.newsaggregator.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.ArticleStatus;
import com.newsaggregator.entity.Category;
import com.newsaggregator.repository.ArticleRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService Unit Tests")
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    private Article testArticle;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Технологии");

        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");
        testArticle.setSummary("Test summary");
        testArticle.setContent("Test content");
        testArticle.setStatus(ArticleStatus.PENDING);
        testArticle.setCategory(testCategory);
        testArticle.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should get article by ID successfully")
    void testGetArticleById_Success() {
        // Arrange
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // Act
        Optional<Article> found = articleService.getArticleById(1L);

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Test Article", found.get().getTitle());
        verify(articleRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should get published articles with pagination")
    void testGetPublishedArticles() {
        // Arrange
        testArticle.setStatus(ArticleStatus.PUBLISHED);
        testArticle.setPublishedAt(LocalDateTime.now());
        List<Article> articles = Arrays.asList(testArticle);
        Page<Article> page = new PageImpl<>(articles);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(articleRepository.findPublishedArticles(pageable)).thenReturn(page);

        // Act
        Page<Article> result = articleService.getPublishedArticles(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(ArticleStatus.PUBLISHED, result.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("Should get articles by status")
    void testGetArticlesByStatus() {
        // Arrange
        List<Article> articles = Arrays.asList(testArticle);
        Page<Article> page = new PageImpl<>(articles);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(articleRepository.findByStatus(ArticleStatus.PENDING, pageable)).thenReturn(page);

        // Act
        Page<Article> result = articleService.getArticlesByStatus(ArticleStatus.PENDING, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(ArticleStatus.PENDING, result.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("Should publish article successfully")
    void testPublishArticle() {
        // Arrange
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);

        // Act
        articleService.publishArticle(1L);

        // Assert
        assertEquals(ArticleStatus.PUBLISHED, testArticle.getStatus());
        verify(articleRepository, times(1)).save(testArticle);
    }

    @Test
    @DisplayName("Should delete article successfully")
    void testDeleteArticle() {
        // Arrange
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        doNothing().when(articleRepository).delete(any(Article.class));

        // Act
        articleService.deleteArticle(1L);

        // Assert
        verify(articleRepository, times(1)).delete(any(Article.class));
    }

    @Test
    @DisplayName("Should get published articles count")
    void testGetPublishedArticlesCount() {
        // Arrange
        when(articleRepository.countByStatus(ArticleStatus.PUBLISHED)).thenReturn(50L);

        // Act
        long count = articleService.getPublishedArticlesCount();

        // Assert
        assertEquals(50L, count);
        verify(articleRepository, times(1)).countByStatus(ArticleStatus.PUBLISHED);
    }
}
