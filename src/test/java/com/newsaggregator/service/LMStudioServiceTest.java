package com.newsaggregator.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("LMStudioService Unit Tests")
class LMStudioServiceTest {

    private LMStudioService lmStudioService;

    @BeforeEach
    void setUp() {
        lmStudioService = new LMStudioService();
        // Устанавливаем значения через рефлексию (ИИ отключен для fallback тестов)
        ReflectionTestUtils.setField(lmStudioService, "enabled", false);
        ReflectionTestUtils.setField(lmStudioService, "apiUrl", "http://localhost:1234/v1");
        ReflectionTestUtils.setField(lmStudioService, "model", "test-model");
        ReflectionTestUtils.setField(lmStudioService, "timeoutSeconds", 60);
    }

    @Test
    @DisplayName("Should categorize article with basic fallback when disabled")
    void testCategorizeArticle_Disabled() {
        // Arrange
        String title = "Новая технология искусственного интеллекта";
        String content = "Компания представила новый программный продукт";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        assertNotNull(category);
        assertEquals("Технологии", category);
    }

    @Test
    @DisplayName("Should generate basic summary when disabled")
    void testGenerateSummary_Disabled() {
        // Arrange
        String content = "Это тестовая статья с коротким содержанием.";

        // Act
        String summary = lmStudioService.generateSummary(content);

        // Assert
        assertNotNull(summary);
        assertTrue(summary.length() > 0);
    }

    @Test
    @DisplayName("Should determine basic category for technology keywords")
    void testDetermineBasicCategory_Technology() {
        // Arrange
        String title = "Новый компьютер с процессором";
        String content = "Программное обеспечение для разработки";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        assertEquals("Технологии", category);
    }

    @Test
    @DisplayName("Should determine basic category for politics keywords")
    void testDetermineBasicCategory_Politics() {
        // Arrange
        String title = "Президент подписал новый закон";
        String content = "Правительство приняло решение о выборах";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        assertEquals("Политика", category);
    }

    @Test
    @DisplayName("Should determine basic category for economy keywords")
    void testDetermineBasicCategory_Economy() {
        // Arrange
        String title = "Курс доллара вырос";
        String content = "Банк России изменил ставку финансирования";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        assertEquals("Экономика", category);
    }

    @Test
    @DisplayName("Should determine basic category for sports keywords")
    void testDetermineBasicCategory_Sports() {
        // Arrange
        String title = "Футбольный матч завершился победой";
        String content = "Чемпионат по хоккею начнется в следующем месяце";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        assertEquals("Спорт", category);
    }

    @Test
    @DisplayName("Should return default category for unknown keywords")
    void testDetermineBasicCategory_Default() {
        // Arrange
        String title = "Обычная новость";
        String content = "Какое-то событие произошло";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        assertEquals("Общество", category);
    }

    @Test
    @DisplayName("Should generate basic summary for short content")
    void testGenerateBasicSummary_ShortContent() {
        // Arrange
        String content = "Короткий текст.";

        // Act
        String summary = lmStudioService.generateSummary(content);

        // Assert
        assertEquals(content, summary);
    }

    @Test
    @DisplayName("Should truncate long content in basic summary")
    void testGenerateBasicSummary_LongContent() {
        // Arrange
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("Это предложение номер ").append(i).append(". ");
        }

        // Act
        String summary = lmStudioService.generateSummary(longContent.toString());

        // Assert
        assertNotNull(summary);
        assertTrue(summary.length() < longContent.length());
    }

    @Test
    @DisplayName("Should return false when API is disabled")
    void testIsApiAvailable_Disabled() {
        // Act
        boolean available = lmStudioService.isApiAvailable();

        // Assert
        assertFalse(available);
    }

    @Test
    @DisplayName("Should return true when service is configured")
    void testIsConfigured_True() {
        // Arrange
        ReflectionTestUtils.setField(lmStudioService, "enabled", true);

        // Act
        boolean configured = lmStudioService.isConfigured();

        // Assert
        assertTrue(configured);
    }

    @Test
    @DisplayName("Should return false when service is not configured")
    void testIsConfigured_False() {
        // Act
        boolean configured = lmStudioService.isConfigured();

        // Assert
        assertFalse(configured);
    }

    @Test
    @DisplayName("Should return empty list when getting models while disabled")
    void testGetAvailableModels_Disabled() {
        // Act
        List<String> models = lmStudioService.getAvailableModels();

        // Assert
        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    @DisplayName("Should validate good quality summary")
    void testIsValidSummary_GoodQuality() {
        // Arrange
        String goodSummary = "Это качественная сводка новости. Она содержит несколько предложений. " +
                "Информация представлена четко и понятно. Сводка имеет достаточную длину.";
        String originalContent = "Длинная статья с большим количеством текста и информации...";

        // Act
        boolean isValid = (boolean) ReflectionTestUtils.invokeMethod(
                lmStudioService, "isValidSummary", goodSummary, originalContent);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject too short summary")
    void testIsValidSummary_TooShort() {
        // Arrange
        String shortSummary = "Короткая сводка.";
        String originalContent = "Длинная статья с большим количеством текста...";

        // Act
        boolean isValid = (boolean) ReflectionTestUtils.invokeMethod(
                lmStudioService, "isValidSummary", shortSummary, originalContent);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject summary without complete sentences")
    void testIsValidSummary_NoCompleteSentences() {
        // Arrange
        String incompleteSummary = "Это сводка без завершения предложений и знаков препинания " +
                "которая продолжается и продолжается без точек или других знаков";
        String originalContent = "Длинная статья...";

        // Act
        boolean isValid = (boolean) ReflectionTestUtils.invokeMethod(
                lmStudioService, "isValidSummary", incompleteSummary, originalContent);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject summary with technical artifacts")
    void testIsValidSummary_WithArtifacts() {
        // Arrange
        String summaryWithArtifacts = "Это сводка с техническими артефактами. ```code``` " +
                "Она содержит markdown разметку. ### Заголовок";
        String originalContent = "Длинная статья...";

        // Act
        boolean isValid = (boolean) ReflectionTestUtils.invokeMethod(
                lmStudioService, "isValidSummary", summaryWithArtifacts, originalContent);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should clean summary by removing prefixes")
    void testCleanSummary_RemovePrefixes() {
        // Arrange
        String summaryWithPrefix = "Сводка: Это основное содержание новости. Важная информация здесь.";

        // Act
        String cleaned = (String) ReflectionTestUtils.invokeMethod(
                lmStudioService, "cleanSummary", summaryWithPrefix);

        // Assert
        assertNotNull(cleaned);
        assertFalse(cleaned.startsWith("Сводка:"));
        assertTrue(cleaned.startsWith("Это основное"));
    }

    @Test
    @DisplayName("Should clean summary by removing intro phrases")
    void testCleanSummary_RemoveIntroPhrases() {
        // Arrange
        String summaryWithIntro = "В статье говорится о том, что произошло важное событие. Детали события.";

        // Act
        String cleaned = (String) ReflectionTestUtils.invokeMethod(
                lmStudioService, "cleanSummary", summaryWithIntro);

        // Assert
        assertNotNull(cleaned);
        assertFalse(cleaned.contains("В статье говорится"));
        assertTrue(cleaned.startsWith("произошло важное"));
    }

    @Test
    @DisplayName("Should truncate very long summary")
    void testCleanSummary_TruncateLong() {
        // Arrange
        StringBuilder longSummary = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longSummary.append("Это предложение номер ").append(i).append(". ");
        }

        // Act
        String cleaned = (String) ReflectionTestUtils.invokeMethod(
                lmStudioService, "cleanSummary", longSummary.toString());

        // Assert
        assertNotNull(cleaned);
        assertTrue(cleaned.length() <= 600);
    }

    @Test
    @DisplayName("Should generate basic summary for very short content")
    void testGenerateBasicSummary_VeryShortContent() {
        // Arrange
        String shortContent = "Короткая новость.";

        // Act
        String summary = (String) ReflectionTestUtils.invokeMethod(
                lmStudioService, "generateBasicSummary", shortContent);

        // Assert
        assertEquals(shortContent, summary);
    }

    @Test
    @DisplayName("Should generate basic summary with 2-3 sentences")
    void testGenerateBasicSummary_MultipleSentences() {
        // Arrange
        String content = "Первое предложение новости с достаточным количеством текста для проверки. " +
                "Второе предложение с деталями и дополнительной информацией о событии. " +
                "Третье предложение с еще большим количеством дополнительной информации. " +
                "Четвертое предложение. Пятое предложение. Шестое предложение.";

        // Act
        String summary = (String) ReflectionTestUtils.invokeMethod(
                lmStudioService, "generateBasicSummary", content);

        // Assert
        assertNotNull(summary);
        assertTrue(summary.length() >= 100); // Проверяем минимальную длину
        assertTrue(summary.contains("Первое предложение"));
    }

    @Test
    @DisplayName("Should categorize with mixed keywords")
    void testDetermineBasicCategory_MixedKeywords() {
        // Arrange
        String title = "Новая технология в медицине";
        String content = "Ученые разработали программное обеспечение для врачей";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        // Должна выбрать первую найденную категорию (Технологии)
        assertEquals("Технологии", category);
    }

    @Test
    @DisplayName("Should categorize health article correctly")
    void testDetermineBasicCategory_Health() {
        // Arrange
        String title = "Новое лечение болезни";
        String content = "Врачи в клинике провели успешное лечение пациента";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        assertEquals("Здоровье", category);
    }

    @Test
    @DisplayName("Should categorize culture article correctly")
    void testDetermineBasicCategory_Culture() {
        // Arrange
        String title = "Новая выставка картин";
        String content = "В музее открылась выставка искусства и кино";

        // Act
        String category = lmStudioService.categorizeArticle(title, content);

        // Assert
        assertEquals("Культура", category);
    }
}
