package com.newsaggregator.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.ArticleStatus;
import com.newsaggregator.entity.Category;
import com.newsaggregator.entity.NewsSource;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    Optional<Article> findBySourceUrl(String sourceUrl);
    
    boolean existsBySourceUrl(String sourceUrl);
    
    List<Article> findByStatus(ArticleStatus status);
    
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);
    
    List<Article> findByStatusAndCategory(ArticleStatus status, Category category);
    
    Page<Article> findByStatusAndCategory(ArticleStatus status, Category category, Pageable pageable);
    
    List<Article> findByCategory(Category category);
    
    Page<Article> findByCategory(Category category, Pageable pageable);
    
    List<Article> findBySource(NewsSource source);
    
    Page<Article> findBySource(NewsSource source, Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' ORDER BY a.publishedAt DESC")
    Page<Article> findPublishedArticles(Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND " +
           "(a.title LIKE %:search% OR a.content LIKE %:search%)")
    Page<Article> searchPublishedArticles(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND " +
           "(:search IS NULL OR :search = '' OR a.title LIKE %:search% OR a.content LIKE %:search%) AND " +
           "(:categoryId IS NULL OR a.category.id = :categoryId) AND " +
           "(:sourceId IS NULL OR a.source.id = :sourceId) AND " +
           "(:dateFrom IS NULL OR :dateFrom = '' OR a.publishedAt >= CAST(:dateFrom AS timestamp)) AND " +
           "(:dateTo IS NULL OR :dateTo = '' OR a.publishedAt <= CAST(:dateTo AS timestamp)) " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'publishedAt' AND :sortDir = 'desc' THEN a.publishedAt END DESC, " +
           "CASE WHEN :sortBy = 'publishedAt' AND :sortDir = 'asc' THEN a.publishedAt END ASC, " +
           "CASE WHEN :sortBy = 'title' AND :sortDir = 'desc' THEN a.title END DESC, " +
           "CASE WHEN :sortBy = 'title' AND :sortDir = 'asc' THEN a.title END ASC, " +
           "a.publishedAt DESC")
    Page<Article> searchPublishedArticlesWithFilters(@Param("search") String search,
                                                     @Param("categoryId") Long categoryId,
                                                     @Param("sourceId") Long sourceId,
                                                     @Param("dateFrom") String dateFrom,
                                                     @Param("dateTo") String dateTo,
                                                     @Param("sortBy") String sortBy,
                                                     @Param("sortDir") String sortDir,
                                                     Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND " +
           "(:categoryId IS NULL OR a.category.id = :categoryId) AND " +
           "(:sourceId IS NULL OR a.source.id = :sourceId) " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'publishedAt' AND :sortDir = 'desc' THEN a.publishedAt END DESC, " +
           "CASE WHEN :sortBy = 'publishedAt' AND :sortDir = 'asc' THEN a.publishedAt END ASC, " +
           "CASE WHEN :sortBy = 'title' AND :sortDir = 'desc' THEN a.title END DESC, " +
           "CASE WHEN :sortBy = 'title' AND :sortDir = 'asc' THEN a.title END ASC, " +
           "a.publishedAt DESC")
    Page<Article> getPublishedArticlesWithFilters(@Param("categoryId") Long categoryId,
                                                  @Param("sourceId") Long sourceId,
                                                  @Param("sortBy") String sortBy,
                                                  @Param("sortDir") String sortDir,
                                                  Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND a.category = :category " +
           "ORDER BY a.publishedAt DESC")
    Page<Article> findPublishedArticlesByCategory(@Param("category") Category category, Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND a.source = :source " +
           "ORDER BY a.publishedAt DESC")
    Page<Article> findPublishedArticlesBySource(@Param("source") NewsSource source, Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND " +
           "a.publishedAt BETWEEN :startDate AND :endDate ORDER BY a.publishedAt DESC")
    Page<Article> findPublishedArticlesByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate, 
                                                   Pageable pageable);
    
    @Query("SELECT sa.article FROM SavedArticle sa WHERE sa.user.id = :userId ORDER BY sa.savedAt DESC")
    Page<Article> findSavedArticlesByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM Article a WHERE a.status = :status")
    long countByStatus(@Param("status") ArticleStatus status);
    
    @Query("SELECT COUNT(a) FROM Article a WHERE a.status = 'PUBLISHED' AND a.publishedAt >= :date")
    long countPublishedSince(@Param("date") LocalDateTime date);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND a.category.id IN :categoryIds " +
           "ORDER BY a.publishedAt DESC")
    Page<Article> findPublishedArticlesByCategories(@Param("categoryIds") List<Long> categoryIds, 
                                                    Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND a.source.id IN :sourceIds " +
           "ORDER BY a.publishedAt DESC")
    Page<Article> findPublishedArticlesBySources(@Param("sourceIds") List<Long> sourceIds, 
                                                 Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND " +
           "a.category.id IN :categoryIds AND a.source.id IN :sourceIds " +
           "ORDER BY a.publishedAt DESC")
    Page<Article> findPublishedArticlesByCategoriesAndSources(@Param("categoryIds") List<Long> categoryIds,
                                                              @Param("sourceIds") List<Long> sourceIds,
                                                              Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.createdBy.id = :userId ORDER BY a.createdAt DESC")
    Page<Article> findArticlesByCreatedBy(@Param("userId") Long userId, Pageable pageable);
    
    Page<Article> findByStatusAndSource(ArticleStatus status, NewsSource source, Pageable pageable);
    
    Page<Article> findByStatusAndCategoryAndSource(ArticleStatus status, Category category, NewsSource source, Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND LENGTH(a.content) < 500 ORDER BY a.publishedAt DESC")
    List<Article> findArticlesWithShortContent();
    
    @Query("SELECT a FROM Article a WHERE " +
           "(LENGTH(a.content) < 300 OR " +
           "a.content LIKE '%Читать далее%' OR " +
           "a.content LIKE '%Read more%' OR " +
           "a.content LIKE '%Continue reading%' OR " +
           "a.content LIKE '%...%') " +
           "ORDER BY a.createdAt DESC")
    List<Article> findIncompleteArticles();
}