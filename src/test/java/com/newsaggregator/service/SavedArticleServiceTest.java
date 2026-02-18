package com.newsaggregator.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.newsaggregator.entity.SavedArticle;
import com.newsaggregator.entity.User;
import com.newsaggregator.repository.ArticleRepository;
import com.newsaggregator.repository.SavedArticleRepository;
import com.newsaggregator.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavedArticleService Unit Tests")
class SavedArticleServiceTest {

    @Mock
    private SavedArticleRepository savedArticleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private SavedArticleService savedArticleService;

    private User testUser;
    private Article testArticle;
    private SavedArticle testSavedArticle;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");

        testSavedArticle = new SavedArticle(testUser, testArticle);
    }

    @Test
    @DisplayName("Should save article successfully")
    void testSaveArticle_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(savedArticleRepository.existsByUserIdAndArticleId(1L, 1L)).thenReturn(false);
        when(savedArticleRepository.save(any(SavedArticle.class))).thenReturn(testSavedArticle);

        // Act
        boolean result = savedArticleService.saveArticle(1L, 1L);

        // Assert
        assertTrue(result);
        verify(savedArticleRepository, times(1)).save(any(SavedArticle.class));
    }

    @Test
    @DisplayName("Should unsave article successfully")
    void testUnsaveArticle_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        doNothing().when(savedArticleRepository).deleteByUserIdAndArticleId(1L, 1L);

        // Act
        boolean result = savedArticleService.unsaveArticle(1L, 1L);

        // Assert
        assertTrue(result);
        verify(savedArticleRepository, times(1)).deleteByUserIdAndArticleId(1L, 1L);
    }

    @Test
    @DisplayName("Should check if article is saved")
    void testIsArticleSaved() {
        // Arrange
        when(savedArticleRepository.existsByUserIdAndArticleId(1L, 1L)).thenReturn(true);
        when(savedArticleRepository.existsByUserIdAndArticleId(1L, 2L)).thenReturn(false);

        // Act & Assert
        assertTrue(savedArticleService.isArticleSaved(1L, 1L));
        assertFalse(savedArticleService.isArticleSaved(1L, 2L));
    }

    @Test
    @DisplayName("Should get saved articles with pagination")
    void testGetSavedArticles() {
        // Arrange
        List<Article> articles = Arrays.asList(testArticle);
        Page<Article> page = new PageImpl<>(articles);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(savedArticleRepository.findSavedArticlesByUserId(1L, pageable)).thenReturn(page);

        // Act
        Page<Article> result = savedArticleService.getSavedArticles(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Should toggle save status - save when not saved")
    void testToggleSaveStatus_Save() {
        // Arrange
        when(savedArticleRepository.existsByUserIdAndArticleId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(savedArticleRepository.save(any(SavedArticle.class))).thenReturn(testSavedArticle);

        // Act
        boolean result = savedArticleService.toggleSaveStatus(1L, 1L);

        // Assert
        assertTrue(result);
        verify(savedArticleRepository, times(1)).save(any(SavedArticle.class));
    }

    @Test
    @DisplayName("Should toggle save status - unsave when saved")
    void testToggleSaveStatus_Unsave() {
        // Arrange
        when(savedArticleRepository.existsByUserIdAndArticleId(1L, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        doNothing().when(savedArticleRepository).deleteByUserIdAndArticleId(1L, 1L);

        // Act
        boolean result = savedArticleService.toggleSaveStatus(1L, 1L);

        // Assert
        assertTrue(result);
        verify(savedArticleRepository, times(1)).deleteByUserIdAndArticleId(1L, 1L);
    }
}
