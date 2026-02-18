package com.newsaggregator.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.SavedArticle;
import com.newsaggregator.entity.User;
import com.newsaggregator.repository.ArticleRepository;
import com.newsaggregator.repository.SavedArticleRepository;
import com.newsaggregator.repository.UserRepository;

/**
 * Сервис для управления сохраненными статьями пользователей.
 * 
 * Предоставляет функциональность для:
 * - Сохранения статей в избранное пользователя
 * - Удаления статей из избранного
 * - Получения списка сохраненных статей
 * - Проверки статуса сохранения статьи
 * - Статистики по сохраненным статьям
 * - Массовых операций с сохраненными статьями
 * 
 * @author News Aggregator Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class SavedArticleService {
    
    private static final Logger logger = LoggerFactory.getLogger(SavedArticleService.class);
    
    @Autowired
    private SavedArticleRepository savedArticleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ArticleRepository articleRepository;
    
    /**
     * Сохраняет статью в избранное пользователя.
     * Если статья уже сохранена, операция игнорируется.
     * 
     * @param userId идентификатор пользователя
     * @param articleId идентификатор статьи
     * @return true, если операция выполнена успешно, false в случае ошибки
     */
    public boolean saveArticle(Long userId, Long articleId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
            
            // Проверяем, не сохранена ли уже статья
            if (savedArticleRepository.existsByUserIdAndArticleId(userId, articleId)) {
                return true; // Уже сохранена
            }
            
            // Создаем новую запись
            SavedArticle savedArticle = new SavedArticle(user, article);
            savedArticleRepository.save(savedArticle);
            
            logger.info("Пользователь {} сохранил статью {}", user.getUsername(), article.getTitle());
            return true;
            
        } catch (Exception e) {
            logger.error("Ошибка при сохранении статьи {} пользователем {}: {}", articleId, userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Удаляет статью из избранного пользователя.
     * 
     * @param userId идентификатор пользователя
     * @param articleId идентификатор статьи
     * @return true, если операция выполнена успешно, false в случае ошибки
     */
    public boolean unsaveArticle(Long userId, Long articleId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
            
            // Удаляем запись, если она существует
            savedArticleRepository.deleteByUserIdAndArticleId(userId, articleId);
            
            logger.info("Пользователь {} удалил из сохраненных статью {}", user.getUsername(), article.getTitle());
            return true;
            
        } catch (Exception e) {
            logger.error("Ошибка при удалении статьи {} из сохраненных пользователя {}: {}", articleId, userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Проверяет, сохранена ли статья пользователем.
     * 
     * @param userId идентификатор пользователя
     * @param articleId идентификатор статьи
     * @return true, если статья сохранена пользователем, false в противном случае
     */
    @Transactional(readOnly = true)
    public boolean isArticleSaved(Long userId, Long articleId) {
        return savedArticleRepository.existsByUserIdAndArticleId(userId, articleId);
    }
    
    /**
     * Получает количество сохраненных статей пользователя.
     * 
     * @param userId идентификатор пользователя
     * @return количество сохраненных статей
     */
    @Transactional(readOnly = true)
    public long getSavedArticlesCount(Long userId) {
        return savedArticleRepository.countByUserId(userId);
    }
    
    /**
     * Получает сохраненные статьи пользователя с пагинацией.
     * 
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница сохраненных статей
     */
    @Transactional(readOnly = true)
    public Page<Article> getSavedArticles(Long userId, Pageable pageable) {
        return savedArticleRepository.findSavedArticlesByUserId(userId, pageable);
    }
    
    /**
     * Получает информацию о сохраненной статье.
     * 
     * @param userId идентификатор пользователя
     * @param articleId идентификатор статьи
     * @return Optional с информацией о сохраненной статье или пустой Optional
     */
    @Transactional(readOnly = true)
    public Optional<SavedArticle> getSavedArticle(Long userId, Long articleId) {
        return savedArticleRepository.findByUserIdAndArticleId(userId, articleId);
    }
    
    /**
     * Удаляет все сохраненные статьи пользователя.
     * Используется при удалении пользователя или очистке избранного.
     * 
     * @param userId идентификатор пользователя
     * @throws RuntimeException если операция не удалась
     */
    public void deleteAllUserSavedArticles(Long userId) {
        try {
            savedArticleRepository.deleteByUserId(userId);
            logger.info("Удалены все сохраненные статьи пользователя {}", userId);
        } catch (Exception e) {
            logger.error("Ошибка при удалении всех сохраненных статей пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException("Не удалось удалить сохраненные статьи: " + e.getMessage());
        }
    }
    
    /**
     * Удаляет все записи сохранения для конкретной статьи.
     * Используется при удалении статьи из системы.
     * 
     * @param articleId идентификатор статьи
     * @throws RuntimeException если операция не удалась
     */
    public void deleteAllSavedEntriesForArticle(Long articleId) {
        try {
            savedArticleRepository.deleteByArticleId(articleId);
            logger.info("Удалены все записи сохранения для статьи {}", articleId);
        } catch (Exception e) {
            logger.error("Ошибка при удалении записей сохранения для статьи {}: {}", articleId, e.getMessage());
            throw new RuntimeException("Не удалось удалить записи сохранения: " + e.getMessage());
        }
    }
    
    /**
     * Получает количество пользователей, сохранивших статью.
     * Используется для статистики популярности статей.
     * 
     * @param articleId идентификатор статьи
     * @return количество пользователей, сохранивших статью
     */
    @Transactional(readOnly = true)
    public long getArticleSaveCount(Long articleId) {
        return savedArticleRepository.countByArticleId(articleId);
    }
    
    /**
     * Переключает статус сохранения статьи.
     * Если статья сохранена - удаляет из избранного, если не сохранена - добавляет.
     * 
     * @param userId идентификатор пользователя
     * @param articleId идентификатор статьи
     * @return true, если операция выполнена успешно, false в случае ошибки
     */
    public boolean toggleSaveStatus(Long userId, Long articleId) {
        if (isArticleSaved(userId, articleId)) {
            return unsaveArticle(userId, articleId);
        } else {
            return saveArticle(userId, articleId);
        }
    }
}