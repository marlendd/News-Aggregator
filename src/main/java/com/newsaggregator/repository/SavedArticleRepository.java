package com.newsaggregator.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.SavedArticle;
import com.newsaggregator.entity.User;

@Repository
public interface SavedArticleRepository extends JpaRepository<SavedArticle, Long> {
    
    /**
     * Найти сохраненную статью по пользователю и статье
     */
    Optional<SavedArticle> findByUserAndArticle(User user, Article article);
    
    /**
     * Найти сохраненную статью по ID пользователя и ID статьи
     */
    @Query("SELECT sa FROM SavedArticle sa WHERE sa.user.id = :userId AND sa.article.id = :articleId")
    Optional<SavedArticle> findByUserIdAndArticleId(@Param("userId") Long userId, @Param("articleId") Long articleId);
    
    /**
     * Проверить, сохранена ли статья пользователем
     */
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);
    
    /**
     * Получить количество сохраненных статей пользователя
     */
    long countByUserId(Long userId);
    
    /**
     * Получить все сохраненные статьи пользователя с пагинацией
     */
    @Query("SELECT sa.article FROM SavedArticle sa WHERE sa.user.id = :userId ORDER BY sa.savedAt DESC")
    Page<Article> findSavedArticlesByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Удалить сохраненную статью по ID пользователя и ID статьи
     */
    void deleteByUserIdAndArticleId(Long userId, Long articleId);
    
    /**
     * Удалить все сохраненные статьи пользователя
     */
    void deleteByUserId(Long userId);
    
    /**
     * Удалить все записи для конкретной статьи (при удалении статьи)
     */
    void deleteByArticleId(Long articleId);
    
    /**
     * Получить последние сохраненные статьи пользователя
     */
    @Query("SELECT sa FROM SavedArticle sa WHERE sa.user.id = :userId ORDER BY sa.savedAt DESC")
    Page<SavedArticle> findRecentSavedArticlesByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Получить статистику по сохраненным статьям
     */
    @Query("SELECT COUNT(sa) FROM SavedArticle sa WHERE sa.article.id = :articleId")
    long countByArticleId(@Param("articleId") Long articleId);
}