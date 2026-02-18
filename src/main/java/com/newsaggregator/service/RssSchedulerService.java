package com.newsaggregator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Сервис для планового парсинга RSS лент.
 * 
 * Предоставляет функциональность для:
 * - Автоматического парсинга RSS лент по расписанию
 * - Парсинга в рабочее время (с 8:00 до 22:00)
 * - Ночного парсинга (в 2:00 каждый день)
 * - Обработки ошибок при плановом парсинге
 * 
 * ВНИМАНИЕ: Все методы планового парсинга временно отключены
 * для ручного управления процессом парсинга.
 * 
 * @author News Aggregator Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class RssSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(RssSchedulerService.class);

    @Autowired
    private RssParserService rssParserService;

    /**
     * Автоматический парсинг RSS лент каждые 30 минут.
     * 
     * ВРЕМЕННО ОТКЛЮЧЕН для ручного управления.
     * Для активации раскомментируйте аннотацию @Scheduled(fixedRate = 30 * 60 * 1000).
     * 
     * Выполняет парсинг всех активных RSS источников с интервалом 30 минут.
     * Обрабатывает ошибки и логирует результаты выполнения.
     */
    // @Scheduled(fixedRate = 30 * 60 * 1000) // 30 минут в миллисекундах
    public void scheduleRssParsing() {
        logger.info("Запуск планового парсинга RSS лент...");
        
        try {
            rssParserService.parseAllRssFeeds();
            logger.info("Плановый парсинг RSS лент завершен успешно");
        } catch (Exception e) {
            logger.error("Ошибка при плановом парсинге RSS лент: {}", e.getMessage(), e);
        }
    }

    /**
     * Парсинг RSS лент каждый час в рабочее время (с 8:00 до 22:00).
     * 
     * ВРЕМЕННО ОТКЛЮЧЕН для ручного управления.
     * Для активации раскомментируйте аннотацию @Scheduled(cron = "0 0 8-22 * * *").
     * 
     * Выполняет парсинг в активное время суток, когда пользователи
     * наиболее активно читают новости.
     */
    // @Scheduled(cron = "0 0 8-22 * * *") // Каждый час с 8:00 до 22:00
    public void scheduleWorkingHoursRssParsing() {
        logger.info("Запуск парсинга RSS лент в рабочее время...");
        
        try {
            rssParserService.parseAllRssFeeds();
            logger.info("Парсинг RSS лент в рабочее время завершен успешно");
        } catch (Exception e) {
            logger.error("Ошибка при парсинге RSS лент в рабочее время: {}", e.getMessage(), e);
        }
    }

    /**
     * Ночной парсинг RSS лент (в 2:00 каждый день).
     * 
     * ВРЕМЕННО ОТКЛЮЧЕН для ручного управления.
     * Для активации раскомментируйте аннотацию @Scheduled(cron = "0 0 2 * * *").
     * 
     * Выполняет полный парсинг всех источников в ночное время,
     * когда нагрузка на систему минимальна.
     */
    // @Scheduled(cron = "0 0 2 * * *") // Каждый день в 2:00
    public void scheduleNightlyRssParsing() {
        logger.info("Запуск ночного парсинга RSS лент...");
        
        try {
            rssParserService.parseAllRssFeeds();
            logger.info("Ночной парсинг RSS лент завершен успешно");
        } catch (Exception e) {
            logger.error("Ошибка при ночном парсинге RSS лент: {}", e.getMessage(), e);
        }
    }
}