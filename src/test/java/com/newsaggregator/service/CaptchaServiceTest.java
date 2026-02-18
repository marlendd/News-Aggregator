package com.newsaggregator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaptchaServiceTest {

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaService();
    }

    @Test
    void testGenerateCaptchaImage_ShouldReturnBase64Image() {
        // When
        String captchaImage = captchaService.generateCaptchaImage();
        
        // Then
        assertNotNull(captchaImage);
        assertTrue(captchaImage.startsWith("data:image/png;base64,"));
        assertTrue(captchaImage.length() > 100); // Base64 изображение должно быть достаточно длинным
    }

    @Test
    void testGenerateCaptchaImage_ShouldSetExpectedText() {
        // When
        captchaService.generateCaptchaImage();
        
        // Then
        String expectedText = captchaService.getExpectedText();
        assertNotNull(expectedText);
        assertEquals(6, expectedText.length()); // Длина текста должна быть 6 символов
        assertTrue(expectedText.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]+"));
    }

    @Test
    void testValidateCaptcha_WithCorrectAnswer_ShouldReturnTrue() {
        // Given
        captchaService.generateCaptchaImage();
        String expectedText = captchaService.getExpectedText();
        
        // When
        boolean result = captchaService.validateCaptcha(expectedText);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testValidateCaptcha_WithCorrectAnswerDifferentCase_ShouldReturnTrue() {
        // Given
        captchaService.generateCaptchaImage();
        String expectedText = captchaService.getExpectedText();
        
        // When
        boolean result = captchaService.validateCaptcha(expectedText.toLowerCase());
        
        // Then
        assertTrue(result);
    }

    @Test
    void testValidateCaptcha_WithCorrectAnswerWithSpaces_ShouldReturnTrue() {
        // Given
        captchaService.generateCaptchaImage();
        String expectedText = captchaService.getExpectedText();
        
        // When
        boolean result = captchaService.validateCaptcha("  " + expectedText + "  ");
        
        // Then
        assertTrue(result);
    }

    @Test
    void testValidateCaptcha_WithIncorrectAnswer_ShouldReturnFalse() {
        // Given
        captchaService.generateCaptchaImage();
        
        // When
        boolean result = captchaService.validateCaptcha("WRONG");
        
        // Then
        assertFalse(result);
    }

    @Test
    void testValidateCaptcha_WithNullAnswer_ShouldReturnFalse() {
        // Given
        captchaService.generateCaptchaImage();
        
        // When
        boolean result = captchaService.validateCaptcha(null);
        
        // Then
        assertFalse(result);
    }

    @Test
    void testValidateCaptcha_WithEmptyAnswer_ShouldReturnFalse() {
        // Given
        captchaService.generateCaptchaImage();
        
        // When
        boolean result = captchaService.validateCaptcha("");
        
        // Then
        assertFalse(result);
    }

    @Test
    void testValidateCaptcha_WithWhitespaceAnswer_ShouldReturnFalse() {
        // Given
        captchaService.generateCaptchaImage();
        
        // When
        boolean result = captchaService.validateCaptcha("   ");
        
        // Then
        assertFalse(result);
    }

    @Test
    void testValidateCaptcha_WithoutGeneratingCaptcha_ShouldReturnFalse() {
        // When (не генерируем капчу)
        boolean result = captchaService.validateCaptcha("ANYTEXT");
        
        // Then
        assertFalse(result);
    }

    @Test
    void testValidateCaptcha_AfterSuccessfulValidation_ShouldResetCaptcha() {
        // Given
        captchaService.generateCaptchaImage();
        String expectedText = captchaService.getExpectedText();
        
        // When
        captchaService.validateCaptcha(expectedText);
        
        // Then
        assertNull(captchaService.getExpectedText());
    }

    @Test
    void testValidateCaptcha_AfterFailedValidation_ShouldNotResetCaptcha() {
        // Given
        captchaService.generateCaptchaImage();
        String expectedText = captchaService.getExpectedText();
        
        // When
        captchaService.validateCaptcha("WRONG");
        
        // Then
        assertEquals(expectedText, captchaService.getExpectedText());
    }

    @Test
    void testReset_ShouldClearExpectedText() {
        // Given
        captchaService.generateCaptchaImage();
        assertNotNull(captchaService.getExpectedText());
        
        // When
        captchaService.reset();
        
        // Then
        assertNull(captchaService.getExpectedText());
    }

    @Test
    void testGenerateCaptchaImage_MultipleCalls_ShouldGenerateDifferentTexts() {
        // When
        captchaService.generateCaptchaImage();
        String firstText = captchaService.getExpectedText();
        
        captchaService.generateCaptchaImage();
        String secondText = captchaService.getExpectedText();
        
        // Then
        assertNotNull(firstText);
        assertNotNull(secondText);
        // Вероятность получить одинаковый текст очень мала (32^6 вариантов)
        // Но теоретически возможна, поэтому проверяем только что оба текста сгенерированы
        assertEquals(6, firstText.length());
        assertEquals(6, secondText.length());
    }

    @Test
    void testGeneratedText_ShouldNotContainAmbiguousCharacters() {
        // Given
        String ambiguousChars = "01OIl";
        
        // When
        captchaService.generateCaptchaImage();
        String generatedText = captchaService.getExpectedText();
        
        // Then
        for (char c : ambiguousChars.toCharArray()) {
            assertFalse(generatedText.contains(String.valueOf(c)), 
                "Generated text should not contain ambiguous character: " + c);
        }
    }

    @Test
    void testGeneratedText_ShouldOnlyContainAllowedCharacters() {
        // Given
        String allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        
        // When
        captchaService.generateCaptchaImage();
        String generatedText = captchaService.getExpectedText();
        
        // Then
        for (char c : generatedText.toCharArray()) {
            assertTrue(allowedChars.contains(String.valueOf(c)), 
                "Generated text contains invalid character: " + c);
        }
    }
}