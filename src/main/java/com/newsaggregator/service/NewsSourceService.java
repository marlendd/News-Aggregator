package com.newsaggregator.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.NewsSource;
import com.newsaggregator.repository.NewsSourceRepository;

/**
 * Сервис для управления источниками новостей (RSS лентами).
 * 
 * Предоставляет функциональность для:
 * - CRUD операций с источниками новостей
 * - Активации и деактивации источников
 * - Управления ошибками парсинга источников
 * - Поиска и фильтрации источников
 * - Статистики по источникам
 * - Сброса ошибок и восстановления источников
 * 
 * @author News Aggregator Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class NewsSourceService {

    @Autowired
    private NewsSourceRepository newsSourceRepository;

    /**
     * Получает все источники новостей без пагинации.
     * 
     * @return список всех источников новостей
     */
    public List<NewsSource> getAllSources() {
        return newsSourceRepository.findAll();
    }

    /**
     * Получает только активные источники новостей.
     * Используется для парсинга RSS лент.
     * 
     * @return список активных источников
     */
    public List<NewsSource> getActiveSources() {
        return newsSourceRepository.findByActiveTrue();
    }

    /**
     * Получает источники новостей с пагинацией.
     * 
     * @param pageable параметры пагинации
     * @return страница источников новостей
     */
    public Page<NewsSource> getAllSources(Pageable pageable) {
        return newsSourceRepository.findAll(pageable);
    }

    /**
     * Находит источник новостей по идентификатору.
     * 
     * @param id идентификатор источника
     * @return Optional с найденным источником или пустой Optional
     */
    public Optional<NewsSource> getSourceById(Long id) {
        return newsSourceRepository.findById(id);
    }

    /**
     * Создает новый источник новостей.
     * 
     * @param name название источника
     * @param rssUrl URL RSS ленты
     * @param websiteUrl URL веб-сайта источника
     * @return созданный источник новостей
     * @throws RuntimeException если источник с таким RSS URL уже существует
     */
    public NewsSource createSource(String name, String rssUrl, String websiteUrl) {
        // Проверяем, не существует ли уже источник с таким RSS URL
        if (newsSourceRepository.existsByRssUrl(rssUrl)) {
            throw new RuntimeException("Источник с таким RSS URL уже существует");
        }

        NewsSource source = new NewsSource();
        source.setName(name);
        source.setRssUrl(rssUrl);
        source.setWebsiteUrl(websiteUrl);
        source.setActive(true);
        source.setErrorCount(0);
        source.setCreatedAt(LocalDateTime.now());

        return newsSourceRepository.save(source);
    }

    /**
     * Обновляет существующий источник новостей.
     * 
     * @param id идентификатор источника для обновления
     * @param name новое название источника
     * @param rssUrl новый URL RSS ленты
     * @param websiteUrl новый URL веб-сайта
     * @param active статус активности источника
     * @return обновленный источник новостей
     * @throws RuntimeException если источник не найден или RSS URL уже используется
     */
    public NewsSource updateSource(Long id, String name, String rssUrl, String websiteUrl, boolean active) {
        NewsSource source = newsSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Источник не найден"));

        // Проверяем, не существует ли уже другой источник с таким RSS URL
        Optional<NewsSource> existingSource = newsSourceRepository.findByRssUrl(rssUrl);
        if (existingSource.isPresent() && !existingSource.get().getId().equals(id)) {
            throw new RuntimeException("Источник с таким RSS URL уже существует");
        }

        source.setName(name);
        source.setRssUrl(rssUrl);
        source.setWebsiteUrl(websiteUrl);
        source.setActive(active);

        return newsSourceRepository.save(source);
    }

    /**
     * Удаляет источник новостей.
     * 
     * @param id идентификатор источника для удаления
     * @throws RuntimeException если источник не найден
     */
    public void deleteSource(Long id) {
        NewsSource source = newsSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Источник не найден"));

        newsSourceRepository.delete(source);
    }

    /**
     * Переключает статус активности источника (активный/неактивный).
     * При активации сбрасывает счетчик ошибок.
     * 
     * @param id идентификатор источника
     * @throws RuntimeException если источник не найден
     */
    public void toggleSourceStatus(Long id) {
        NewsSource source = newsSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Источник не найден"));

        source.setActive(!source.isActive());
        
        // Сбрасываем счетчик ошибок при активации
        if (source.isActive()) {
            source.setErrorCount(0);
            source.setLastError(null);
        }

        newsSourceRepository.save(source);
    }

    /**
     * Сбрасывает ошибки источника и активирует его.
     * Используется для восстановления источников после устранения проблем.
     * 
     * @param id идентификатор источника
     * @throws RuntimeException если источник не найден
     */
    public void resetSourceErrors(Long id) {
        NewsSource source = newsSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Источник не найден"));

        source.setErrorCount(0);
        source.setLastError(null);
        source.setActive(true);

        newsSourceRepository.save(source);
    }

    /**
     * Получает общее количество источников новостей.
     * 
     * @return количество всех источников
     */
    public long getTotalSourcesCount() {
        return newsSourceRepository.count();
    }

    /**
     * Получает количество активных источников новостей.
     * 
     * @return количество активных источников
     */
    public long getActiveSourcesCount() {
        return newsSourceRepository.countByActiveTrue();
    }

    /**
     * Выполняет поиск источников по названию.
     * 
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return страница найденных источников
     */
    public Page<NewsSource> searchSources(String query, Pageable pageable) {
        return newsSourceRepository.findByNameContainingIgnoreCase(query, pageable);
    }
}