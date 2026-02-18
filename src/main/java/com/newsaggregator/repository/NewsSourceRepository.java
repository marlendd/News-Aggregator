package com.newsaggregator.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.newsaggregator.entity.NewsSource;

@Repository
public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {

    /**
     * Найти все активные источники
     */
    List<NewsSource> findByActiveTrue();

    /**
     * Найти источник по RSS URL
     */
    Optional<NewsSource> findByRssUrl(String rssUrl);

    /**
     * Проверить существование источника по RSS URL
     */
    boolean existsByRssUrl(String rssUrl);

    /**
     * Подсчитать количество активных источников
     */
    long countByActiveTrue();

    /**
     * Поиск источников по имени (игнорируя регистр)
     */
    Page<NewsSource> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Найти источники с ошибками
     */
    @Query("SELECT s FROM NewsSource s WHERE s.errorCount > 0")
    List<NewsSource> findSourcesWithErrors();

    /**
     * Найти источники, которые давно не обновлялись
     */
    @Query("SELECT s FROM NewsSource s WHERE s.active = true AND (s.lastUpdated IS NULL OR s.lastUpdated < :cutoffDate)")
    List<NewsSource> findStaleActiveSources(@org.springframework.data.repository.query.Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}