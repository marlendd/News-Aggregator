package com.newsaggregator.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.Category;
import com.newsaggregator.repository.CategoryRepository;

/**
 * Сервис для управления категориями новостей.
 * Предоставляет функционал для создания, обновления, удаления и поиска категорий.
 * Обеспечивает валидацию данных и логирование операций.
 */
@Service
@Transactional
public class CategoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    /**
     * Получение категории по идентификатору.
     * @param id идентификатор категории
     * @return Optional с категорией или пустой Optional, если категория не найдена
     */
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
    
    /**
     * Получение категории по названию.
     * @param name название категории
     * @return Optional с категорией или пустой Optional, если категория не найдена
     */
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }
    
    /**
     * Получение всех категорий, отсортированных по названию.
     * @return список всех категорий
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllOrderByName();
    }
    
    /**
     * Получение категорий, отсортированных по количеству статей.
     * Используется для отображения популярных категорий.
     * @return список категорий, отсортированных по количеству статей
     */
    @Transactional(readOnly = true)
    public List<Category> getCategoriesOrderByArticleCount() {
        return categoryRepository.findCategoriesOrderByArticleCount();
    }
    
    /**
     * Поиск категорий по названию (регистронезависимый).
     * @param name часть названия для поиска
     * @return список найденных категорий
     */
    @Transactional(readOnly = true)
    public List<Category> searchCategories(String name) {
        return categoryRepository.findByNameContainingIgnoreCase(name);
    }
    
    /**
     * Проверка существования категории с указанным названием.
     * @param name название категории
     * @return true, если категория существует
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }
    
    /**
     * Создание новой категории.
     * Проверяет уникальность названия перед созданием.
     * @param name название категории
     * @param description описание категории
     * @param color цветовой код для визуального оформления
     * @return созданная категория
     * @throws IllegalArgumentException если категория с таким названием уже существует
     */
    public Category createCategory(String name, String description, String color) {
        if (existsByName(name)) {
            throw new IllegalArgumentException("Категория с таким названием уже существует");
        }
        
        Category category = new Category(name, description, color);
        Category savedCategory = categoryRepository.save(category);
        logger.info("Создана новая категория: {} (ID: {})", name, savedCategory.getId());
        return savedCategory;
    }
    
    /**
     * Обновление существующей категории.
     * Проверяет уникальность названия при изменении.
     * @param categoryId идентификатор категории
     * @param name новое название
     * @param description новое описание
     * @param color новый цветовой код
     * @return обновленная категория
     * @throws IllegalArgumentException если категория не найдена или название уже используется
     */
    public Category updateCategory(Long categoryId, String name, String description, String color) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
        
        // Проверяем уникальность названия только если оно изменилось
        if (!category.getName().equals(name) && existsByName(name)) {
            throw new IllegalArgumentException("Категория с таким названием уже существует");
        }
        
        category.setName(name);
        category.setDescription(description);
        category.setColorCode(color);
        
        Category savedCategory = categoryRepository.save(category);
        logger.info("Обновлена категория: {} (ID: {})", name, categoryId);
        return savedCategory;
    }
    
    /**
     * Удаление категории.
     * Проверяет, что в категории нет статей перед удалением.
     * @param categoryId идентификатор категории
     * @throws IllegalArgumentException если категория не найдена
     * @throws IllegalStateException если в категории есть статьи
     */
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
        
        // Защита от удаления категорий со статьями
        if (category.getArticleCount() > 0) {
            throw new IllegalStateException("Нельзя удалить категорию, содержащую статьи");
        }
        
        categoryRepository.delete(category);
        logger.info("Удалена категория: {} (ID: {})", category.getName(), categoryId);
    }
    
    /**
     * Получение общего количества категорий.
     * @return количество категорий в системе
     */
    @Transactional(readOnly = true)
    public long getTotalCategoriesCount() {
        return categoryRepository.count();
    }
    
    /**
     * Поиск существующей категории или создание новой.
     * Используется при автоматическом парсинге RSS для создания категорий "на лету".
     * @param name название категории
     * @return найденная или созданная категория
     */
    public Category findOrCreateCategory(String name) {
        return getCategoryByName(name)
            .orElseGet(() -> createCategory(name, "Автоматически созданная категория", "#6c757d"));
    }
}