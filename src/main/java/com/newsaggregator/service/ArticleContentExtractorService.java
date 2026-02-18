package com.newsaggregator.service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Сервис для извлечения полного контента статей с веб-страниц.
 * 
 * Использует библиотеку JSoup для парсинга HTML и извлечения текста статей.
 * Предоставляет функциональность для:
 * - Извлечения полного текста статей по URL
 * - Специализированной обработки популярных новостных сайтов
 * - Извлечения изображений статей (Open Graph, Twitter Card, img теги)
 * - Универсального парсинга для неизвестных сайтов
 * - Очистки текста от навигации и рекламы
 * - Обработки ошибок сети и таймаутов
 * - Валидации размеров и качества изображений
 * 
 * Поддерживает специальные селекторы для:
 * Habr, TechCrunch, Ведомости, Газета.ру, РИА Новости, Лента.ру, BBC, Reuters
 * 
 * @author News Aggregator Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class ArticleContentExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(ArticleContentExtractorService.class);
    
    private static final int TIMEOUT_MS = 10000; // 10 секунд
    private static final int MAX_CONTENT_LENGTH = 50000; // Максимальная длина контента
    
    /**
     * Извлекает полный текст статьи по URL
     */
    public String extractFullContent(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            logger.debug("Извлечение контента из: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .followRedirects(true)
                    .get();
            
            String content = extractContentByDomain(doc, url);
            
            if (content != null && !content.trim().isEmpty()) {
                // Ограничиваем длину контента
                if (content.length() > MAX_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_CONTENT_LENGTH) + "...";
                }
                
                logger.debug("Успешно извлечен контент длиной {} символов", content.length());
                return cleanText(content);
            }
            
            logger.warn("Не удалось извлечь контент из: {}", url);
            return null;
            
        } catch (SocketTimeoutException e) {
            logger.warn("Таймаут при загрузке: {}", url);
            return null;
        } catch (UnknownHostException e) {
            logger.warn("Неизвестный хост: {}", url);
            return null;
        } catch (IOException e) {
            logger.warn("Ошибка при загрузке {}: {}", url, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при извлечении контента из {}: {}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Извлекает контент в зависимости от домена сайта
     */
    private String extractContentByDomain(Document doc, String url) {
        String domain = extractDomain(url);
        
        switch (domain) {
            case "habr.com":
                return extractHabrContent(doc);
            case "techcrunch.com":
                return extractTechCrunchContent(doc);
            case "vedomosti.ru":
                return extractVedomostiContent(doc);
            case "gazeta.ru":
                return extractGazetaContent(doc);
            case "ria.ru":
                return extractRiaContent(doc);
            case "lenta.ru":
                return extractLentaContent(doc);
            case "bbc.com":
            case "bbc.co.uk":
                return extractBBCContent(doc);
            case "reuters.com":
                return extractReutersContent(doc);
            default:
                return extractGenericContent(doc);
        }
    }
    
    /**
     * Извлекает домен из URL
     */
    private String extractDomain(String url) {
        try {
            String domain = url.toLowerCase();
            if (domain.startsWith("http://")) {
                domain = domain.substring(7);
            } else if (domain.startsWith("https://")) {
                domain = domain.substring(8);
            }
            
            if (domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            
            int slashIndex = domain.indexOf('/');
            if (slashIndex > 0) {
                domain = domain.substring(0, slashIndex);
            }
            
            return domain;
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Извлекает контент с Хабра
     */
    private String extractHabrContent(Document doc) {
        // Новые селекторы для Хабра (2026)
        Elements content = doc.select("div.tm-article-body, div.article-formatted-body, div.post__text-html, div.post__body");
        if (!content.isEmpty()) {
            return content.first().text();
        }
        
        // Пробуем альтернативные селекторы
        content = doc.select(".tm-article-snippet__lead-image + div, .post-content__text");
        if (!content.isEmpty()) {
            return content.first().text();
        }
        
        // Fallback - ищем все параграфы внутри статьи
        content = doc.select("article p, .post p");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element p : content) {
                String pText = p.text();
                if (pText.length() > 20) {
                    text.append(pText).append("\n\n");
                }
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        return extractGenericContent(doc);
    }
    
    /**
     * Извлекает контент с TechCrunch
     */
    private String extractTechCrunchContent(Document doc) {
        Elements content = doc.select(".article-content, .entry-content, .post-content");
        if (!content.isEmpty()) {
            return content.first().text();
        }
        
        return extractGenericContent(doc);
    }
    
    /**
     * Извлекает контент с Ведомостей
     */
    private String extractVedomostiContent(Document doc) {
        // Новые селекторы для Ведомостей (2026)
        Elements content = doc.select("div.article__text, div.article__body, div.box-paragraph, div.article-content");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element element : content) {
                text.append(element.text()).append("\n\n");
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        // Пробуем альтернативные селекторы
        content = doc.select(".article p, .content p");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element p : content) {
                String pText = p.text();
                if (pText.length() > 20) {
                    text.append(pText).append("\n\n");
                }
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        return extractGenericContent(doc);
    }
    
    /**
     * Извлекает контент с Газета.ру
     */
    private String extractGazetaContent(Document doc) {
        // Селекторы для Газета.ру
        Elements content = doc.select("div.article_text, div.b-article-text, div.article-text, div.material-text");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element element : content) {
                text.append(element.text()).append("\n\n");
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        // Пробуем альтернативные селекторы
        content = doc.select(".article p, .material p, .content p");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element p : content) {
                String pText = p.text();
                if (pText.length() > 20 && !isGazetaNavigationText(pText)) {
                    text.append(pText).append("\n\n");
                }
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        return extractGenericContent(doc);
    }
    
    /**
     * Извлекает контент с РИА Новости
     */
    private String extractRiaContent(Document doc) {
        // Селекторы для РИА Новости
        Elements content = doc.select("div.article__text, div.article__body, div.article-text, div.layout-article__text");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element element : content) {
                text.append(element.text()).append("\n\n");
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        // Пробуем альтернативные селекторы
        content = doc.select(".article p, .layout-article p");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element p : content) {
                String pText = p.text();
                if (pText.length() > 20) {
                    text.append(pText).append("\n\n");
                }
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        return extractGenericContent(doc);
    }
    
    /**
     * Извлекает контент с Лента.ру
     */
    private String extractLentaContent(Document doc) {
        // Селекторы для Лента.ру
        Elements content = doc.select("div.topic-body__content, div.b-text, div.article-text, div.topic-body");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element element : content) {
                text.append(element.text()).append("\n\n");
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        // Пробуем альтернативные селекторы
        content = doc.select(".topic p, .article p");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element p : content) {
                String pText = p.text();
                if (pText.length() > 20) {
                    text.append(pText).append("\n\n");
                }
            }
            String result = text.toString().trim();
            if (result.length() > 200) {
                return result;
            }
        }
        
        return extractGenericContent(doc);
    }
    
    /**
     * Извлекает контент с BBC
     */
    private String extractBBCContent(Document doc) {
        Elements content = doc.select("[data-component='text-block'], .story-body__inner, .article-body");
        if (!content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Element element : content) {
                text.append(element.text()).append(" ");
            }
            return text.toString().trim();
        }
        
        return extractGenericContent(doc);
    }
    
    /**
     * Извлекает контент с Reuters
     */
    private String extractReutersContent(Document doc) {
        Elements content = doc.select(".ArticleBodyWrapper, .StandardArticleBody_body, .article-body");
        if (!content.isEmpty()) {
            return content.first().text();
        }
        
        return extractGenericContent(doc);
    }
    
    /**
     * Универсальный метод извлечения контента
     */
    private String extractGenericContent(Document doc) {
        // Пробуем стандартные селекторы для статей
        String[] selectors = {
            "article",
            "[role='article']",
            ".article-content",
            ".post-content", 
            ".entry-content",
            ".content",
            ".article-body",
            ".post-body",
            ".story-content",
            "main article",
            ".main-content",
            "#content",
            ".text-content"
        };
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                Element element = elements.first();
                String text = element.text();
                if (text.length() > 300) { // Минимальная длина для валидного контента
                    return text;
                }
            }
        }
        
        // Попытка 2: собираем все параграфы из основного контента
        Elements mainContent = doc.select("main, article, .content, #content");
        if (!mainContent.isEmpty()) {
            Elements paragraphs = mainContent.first().select("p");
            if (paragraphs.size() > 2) {
                StringBuilder content = new StringBuilder();
                for (Element p : paragraphs) {
                    String text = p.text();
                    if (text.length() > 20) { // Игнорируем короткие параграфы
                        content.append(text).append("\n\n");
                    }
                }
                
                String result = content.toString().trim();
                if (result.length() > 300) {
                    return result;
                }
            }
        }
        
        // Последняя попытка - берем все параграфы со страницы
        Elements paragraphs = doc.select("p");
        if (paragraphs.size() > 3) {
            StringBuilder content = new StringBuilder();
            int validParagraphs = 0;
            
            for (Element p : paragraphs) {
                String text = p.text();
                // Игнорируем короткие параграфы и навигационные элементы
                if (text.length() > 50 && !isNavigationText(text)) {
                    content.append(text).append("\n\n");
                    validParagraphs++;
                    
                    // Ограничиваем количество параграфов
                    if (validParagraphs >= 20) {
                        break;
                    }
                }
            }
            
            String result = content.toString().trim();
            if (result.length() > 300) {
                return result;
            }
        }
        
        return null;
    }
    
    /**
     * Проверяет, является ли текст навигационным элементом для Газета.ру
     */
    private boolean isGazetaNavigationText(String text) {
        String lowerText = text.toLowerCase();
        String[] gazetaNavigationKeywords = {
            "подписаться", "читать далее", "комментарии", "поделиться", 
            "версия для печати", "архив", "рубрики", "теги", "реклама",
            "все новости", "главные новости", "лента новостей"
        };
        
        for (String keyword : gazetaNavigationKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Проверяет, является ли текст навигационным элементом
     */
    private boolean isNavigationText(String text) {
        String lowerText = text.toLowerCase();
        String[] navigationKeywords = {
            "cookie", "privacy policy", "terms of service", "subscribe", 
            "newsletter", "follow us", "share", "tweet", "facebook",
            "меню", "навигация", "подписаться", "поделиться", "реклама"
        };
        
        for (String keyword : navigationKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Очищает текст от лишних символов
     */
    private String cleanText(String text) {
        if (text == null) return null;
        
        // Убираем лишние пробелы и переносы строк
        text = text.replaceAll("\\s+", " ");
        text = text.replaceAll("\\n\\s*\\n", "\n\n");
        
        // Убираем служебную информацию
        text = text.replaceAll("(?i)(реклама|advertisement|sponsored|читать далее|read more|подписаться|subscribe)", "");
        
        return text.trim();
    }
    
    /**
     * Проверяет, стоит ли извлекать полный контент для данного URL
     */
    public boolean shouldExtractContent(String url) {
        // Всегда пытаемся извлечь полный контент
        return url != null && !url.trim().isEmpty();
    }
    
    /**
     * Извлекает URL главного изображения статьи с веб-страницы
     */
    public String extractMainImage(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            logger.debug("Извлечение изображения из: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .followRedirects(true)
                    .get();
            
            // 1. Проверяем Open Graph изображение
            Element ogImage = doc.selectFirst("meta[property=og:image]");
            if (ogImage != null) {
                String imageUrl = ogImage.attr("content");
                if (isValidImageUrl(imageUrl)) {
                    logger.debug("Найдено OG изображение: {}", imageUrl);
                    return makeAbsoluteUrl(imageUrl, url);
                }
            }
            
            // 2. Проверяем Twitter Card изображение
            Element twitterImage = doc.selectFirst("meta[name=twitter:image]");
            if (twitterImage != null) {
                String imageUrl = twitterImage.attr("content");
                if (isValidImageUrl(imageUrl)) {
                    logger.debug("Найдено Twitter изображение: {}", imageUrl);
                    return makeAbsoluteUrl(imageUrl, url);
                }
            }
            
            // 3. Ищем первое крупное изображение в статье
            Elements images = doc.select("article img, .article-content img, .post-content img, .entry-content img, .content img");
            for (Element img : images) {
                String imageUrl = img.attr("src");
                if (imageUrl.isEmpty()) {
                    imageUrl = img.attr("data-src"); // Lazy loading
                }
                
                if (isValidImageUrl(imageUrl) && isLargeEnoughImage(img)) {
                    logger.debug("Найдено изображение в статье: {}", imageUrl);
                    return makeAbsoluteUrl(imageUrl, url);
                }
            }
            
            // 4. Ищем любое подходящее изображение
            Elements allImages = doc.select("img");
            for (Element img : allImages) {
                String imageUrl = img.attr("src");
                if (imageUrl.isEmpty()) {
                    imageUrl = img.attr("data-src");
                }
                
                if (isValidImageUrl(imageUrl) && isLargeEnoughImage(img) && !isIconOrLogo(imageUrl)) {
                    logger.debug("Найдено подходящее изображение: {}", imageUrl);
                    return makeAbsoluteUrl(imageUrl, url);
                }
            }
            
        } catch (Exception e) {
            logger.warn("Ошибка при извлечении изображения из '{}': {}", url, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Проверяет, является ли URL валидным URL изображения
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        url = url.trim();
        
        // Проверяем расширение файла
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("?")) {
            lowerUrl = lowerUrl.substring(0, lowerUrl.indexOf("?"));
        }
        
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || 
               lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") || 
               lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".svg") ||
               url.contains("image") || url.length() > 30;
    }
    
    /**
     * Проверяет, достаточно ли большое изображение
     */
    private boolean isLargeEnoughImage(Element img) {
        try {
            String width = img.attr("width");
            String height = img.attr("height");
            
            if (!width.isEmpty() && !height.isEmpty()) {
                int w = Integer.parseInt(width);
                int h = Integer.parseInt(height);
                return w >= 200 && h >= 150; // Минимальные размеры
            }
        } catch (NumberFormatException e) {
            // Игнорируем ошибки парсинга
        }
        
        // Если размеры не указаны, считаем изображение подходящим
        return true;
    }
    
    /**
     * Проверяет, является ли изображение иконкой или логотипом
     */
    private boolean isIconOrLogo(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("icon") || lowerUrl.contains("logo") || 
               lowerUrl.contains("favicon") || lowerUrl.contains("avatar") ||
               lowerUrl.contains("thumb") && lowerUrl.contains("small");
    }
    
    /**
     * Преобразует относительный URL в абсолютный
     */
    private String makeAbsoluteUrl(String imageUrl, String baseUrl) {
        if (imageUrl.startsWith("http")) {
            return imageUrl;
        }
        
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL absolute = new java.net.URL(base, imageUrl);
            return absolute.toString();
        } catch (Exception e) {
            logger.debug("Ошибка при создании абсолютного URL: {}", e.getMessage());
            return imageUrl;
        }
    }
}