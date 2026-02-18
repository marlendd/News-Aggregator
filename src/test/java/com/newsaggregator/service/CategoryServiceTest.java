package com.newsaggregator.service;

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

import com.newsaggregator.entity.Category;
import com.newsaggregator.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Технологии");
        testCategory.setDescription("Новости технологий");
        testCategory.setColorCode("#007bff");
    }

    @Test
    @DisplayName("Should get all categories")
    void testGetAllCategories() {
        // Arrange
        Category cat1 = new Category();
        cat1.setId(1L);
        cat1.setName("Технологии");
        cat1.setDescription("Новости технологий");
        cat1.setColorCode("#007bff");
        
        Category cat2 = new Category();
        cat2.setId(2L);
        cat2.setName("Спорт");
        cat2.setDescription("Спортивные новости");
        cat2.setColorCode("#28a745");
        
        List<Category> categories = Arrays.asList(cat1, cat2);
        when(categoryRepository.findAllOrderByName()).thenReturn(categories);

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(categoryRepository, times(1)).findAllOrderByName();
    }

    @Test
    @DisplayName("Should get category by ID")
    void testGetCategoryById() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act
        Optional<Category> result = categoryService.getCategoryById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Технологии", result.get().getName());
    }

    @Test
    @DisplayName("Should create category successfully")
    void testCreateCategory() {
        // Arrange
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        Category created = categoryService.createCategory("Технологии", "Новости технологий", "#007bff");

        // Assert
        assertNotNull(created);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Should update category successfully")
    void testUpdateCategory() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        categoryService.updateCategory(1L, "Новое имя", "Новое описание", "#ff0000");

        // Assert
        assertEquals("Новое имя", testCategory.getName());
        assertEquals("Новое описание", testCategory.getDescription());
        assertEquals("#ff0000", testCategory.getColorCode());
        verify(categoryRepository, times(1)).save(testCategory);
    }

    @Test
    @DisplayName("Should delete category successfully")
    void testDeleteCategory() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        doNothing().when(categoryRepository).delete(any(Category.class));

        // Act
        categoryService.deleteCategory(1L);

        // Assert
        verify(categoryRepository, times(1)).delete(any(Category.class));
    }

    @Test
    @DisplayName("Should get total categories count")
    void testGetTotalCategoriesCount() {
        // Arrange
        when(categoryRepository.count()).thenReturn(5L);

        // Act
        long count = categoryService.getTotalCategoriesCount();

        // Assert
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("Should search categories by name")
    void testSearchCategories() {
        // Arrange
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.findByNameContainingIgnoreCase("техн")).thenReturn(categories);

        // Act
        List<Category> result = categoryService.searchCategories("техн");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getName().toLowerCase().contains("техн"));
    }
}
