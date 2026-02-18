package com.newsaggregator.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Сервис для генерации и проверки CAPTCHA.
 * Создает графические изображения с текстом для защиты от автоматических атак.
 * Использует session scope для хранения ожидаемого ответа в рамках пользовательской сессии.
 */
@Service
@SessionScope
public class CaptchaService {
    
    private final Random random = new Random();
    private String expectedText; // Ожидаемый текст для текущей сессии
    
    // Настройки изображения CAPTCHA
    private static final int WIDTH = 200;
    private static final int HEIGHT = 60;
    private static final int TEXT_LENGTH = 6;
    
    // Символы для генерации (исключены похожие: 0/O, 1/I/l для лучшей читаемости)
    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    
    /**
     * Генерирует новую CAPTCHA и возвращает изображение в формате Base64.
     * Создает случайный текст, рисует его с искажениями и помехами.
     * @return строка Base64 с изображением CAPTCHA
     */
    public String generateCaptchaImage() {
        // Генерируем случайный текст для новой CAPTCHA
        expectedText = generateRandomText();
        
        // Создаем буферизованное изображение
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Включаем сглаживание для лучшего качества
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Заливаем белый фон
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Добавляем визуальные помехи для усложнения автоматического распознавания
        addNoise(g2d);
        addLines(g2d);
        
        // Рисуем основной текст с искажениями
        drawText(g2d, expectedText);
        
        g2d.dispose();
        
        // Конвертируем изображение в Base64 для передачи в браузер
        return imageToBase64(image);
    }
    
    /**
     * Генерирует случайный текст заданной длины для CAPTCHA.
     * @return случайная строка из допустимых символов
     */
    private String generateRandomText() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < TEXT_LENGTH; i++) {
            text.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return text.toString();
    }
    
    /**
     * Добавляет случайные точки-помехи на изображение.
     * Усложняет автоматическое распознавание текста.
     * @param g2d графический контекст для рисования
     */
    private void addNoise(Graphics2D g2d) {
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            int rgb = random.nextInt(255);
            g2d.setColor(new Color(rgb, rgb, rgb));
            g2d.fillOval(x, y, 2, 2);
        }
    }
    
    /**
     * Добавляет случайные линии-помехи на изображение.
     * Создает дополнительные визуальные препятствия для ботов.
     * @param g2d графический контекст для рисования
     */
    private void addLines(Graphics2D g2d) {
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            
            // Используем приглушенные цвета для линий
            int r = random.nextInt(100) + 100;
            int gr = random.nextInt(100) + 100;
            int b = random.nextInt(100) + 100;
            g2d.setColor(new Color(r, gr, b));
            g2d.drawLine(x1, y1, x2, y2);
        }
    }
    
    /**
     * Рисует текст CAPTCHA с различными искажениями.
     * Применяет случайные цвета, наклоны и смещения для каждого символа.
     * @param g2d графический контекст для рисования
     * @param text текст для отображения
     */
    private void drawText(Graphics2D g2d, String text) {
        Font font = new Font("Arial", Font.BOLD, 30);
        g2d.setFont(font);
        
        int x = 10;
        for (int i = 0; i < text.length(); i++) {
            // Случайный темный цвет для каждой буквы
            int r = random.nextInt(100);
            int gr = random.nextInt(100);
            int b = random.nextInt(100);
            g2d.setColor(new Color(r, gr, b));
            
            // Случайный наклон символа (-10° до +10°)
            int angle = random.nextInt(20) - 10;
            g2d.rotate(Math.toRadians(angle), x + 15, HEIGHT / 2);
            
            // Случайное вертикальное смещение
            int y = HEIGHT / 2 + random.nextInt(10) - 5;
            
            g2d.drawString(String.valueOf(text.charAt(i)), x, y + 10);
            
            // Возвращаем наклон обратно для следующего символа
            g2d.rotate(-Math.toRadians(angle), x + 15, HEIGHT / 2);
            
            x += 28; // Смещение для следующего символа
        }
    }
    
    /**
     * Конвертирует BufferedImage в строку Base64 для передачи в браузер.
     * @param image изображение для конвертации
     * @return строка Base64 с префиксом data:image/png;base64,
     * @throws RuntimeException если произошла ошибка при кодировании
     */
    private String imageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при генерации изображения капчи", e);
        }
    }
    
    /**
     * Проверяет ответ пользователя на CAPTCHA.
     * Сравнение производится без учета регистра.
     * После успешной проверки CAPTCHA сбрасывается.
     * @param userAnswer ответ пользователя
     * @return true, если ответ правильный
     */
    public boolean validateCaptcha(String userAnswer) {
        if (expectedText == null || userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        
        boolean isValid = expectedText.equalsIgnoreCase(userAnswer.trim());
        
        // После проверки сбрасываем CAPTCHA для предотвращения повторного использования
        if (isValid) {
            reset();
        }
        
        return isValid;
    }
    
    /**
     * Получить ожидаемый текст CAPTCHA.
     * Используется для отладки и тестирования.
     * @return ожидаемый текст или null, если CAPTCHA не сгенерирована
     */
    public String getExpectedText() {
        return expectedText;
    }
    
    /**
     * Сбрасывает текущую CAPTCHA.
     * Очищает ожидаемый текст, требуя генерации новой CAPTCHA.
     */
    public void reset() {
        expectedText = null;
    }
}
