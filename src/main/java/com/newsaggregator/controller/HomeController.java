package com.newsaggregator.controller;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.Category;
import com.newsaggregator.entity.User;
import com.newsaggregator.service.ArticleService;
import com.newsaggregator.service.CaptchaService;
import com.newsaggregator.service.CategoryService;
import com.newsaggregator.service.NewsSourceService;
import com.newsaggregator.service.SavedArticleService;
import com.newsaggregator.service.UserService;

@Controller
public class HomeController {
    
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private SavedArticleService savedArticleService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private NewsSourceService newsSourceService;
    
    @Autowired
    private CaptchaService captchaService;
    
    @GetMapping({"/", "/home"})
    public String home(Model model, 
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      Principal principal) {
        
        Page<Article> articles;
        final User user;
        
        // Получаем пользователя и его предпочтения
        if (principal != null) {
            user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                // Используем предпочтения пользователя для размера страницы
                com.newsaggregator.entity.UserPreferences preferences = user.getPreferences();
                if (preferences != null && preferences.getArticlesPerPage() > 0) {
                    size = preferences.getArticlesPerPage();
                }
            }
        } else {
            user = null;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Персонализированная лента для авторизованных пользователей
        if (user != null) {
            articles = articleService.getPersonalizedArticles(user, pageable);
        } else {
            articles = articleService.getPublishedArticles(pageable);
        }
        
        List<Category> categories = categoryService.getAllCategories();
        
        // Получаем информацию о сохраненных статьях для авторизованного пользователя
        if (user != null) {
            List<Long> savedArticleIds = articles.getContent().stream()
                .filter(article -> savedArticleService.isArticleSaved(user.getId(), article.getId()))
                .map(Article::getId)
                .toList();
            model.addAttribute("savedArticleIds", savedArticleIds);
            model.addAttribute("isPersonalized", true);
        }
        
        model.addAttribute("articles", articles);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("pageTitle", "Главная");
        
        return "index";
    }
    
    @GetMapping("/news")
    public String news(Model model,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      @RequestParam(required = false) Long categoryId,
                      @RequestParam(required = false) Long sourceId,
                      @RequestParam(defaultValue = "publishedAt") String sortBy,
                      @RequestParam(defaultValue = "desc") String sortDir,
                      Principal principal) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles;
        
        if (categoryId != null || sourceId != null || !sortBy.equals("publishedAt") || !sortDir.equals("desc")) {
            // Используем фильтрованный поиск
            articles = articleService.getPublishedArticlesWithFilters(categoryId, sourceId, sortBy, sortDir, pageable);
        } else {
            articles = articleService.getPublishedArticles(pageable);
        }
        
        List<Category> categories = categoryService.getAllCategories();
        List<com.newsaggregator.entity.NewsSource> sources = newsSourceService.getActiveSources();
        
        // Получаем информацию о сохраненных статьях для авторизованного пользователя
        if (principal != null) {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                List<Long> savedArticleIds = articles.getContent().stream()
                    .filter(article -> savedArticleService.isArticleSaved(user.getId(), article.getId()))
                    .map(Article::getId)
                    .toList();
                model.addAttribute("savedArticleIds", savedArticleIds);
            }
        }
        
        // Хлебные крошки
        List<com.newsaggregator.util.Breadcrumb> breadcrumbs = new java.util.ArrayList<>();
        breadcrumbs.add(new com.newsaggregator.util.Breadcrumb("Новости"));
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        model.addAttribute("articles", articles);
        model.addAttribute("categories", categories);
        model.addAttribute("sources", sources);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSourceId", sourceId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("pageTitle", "Новости");
        
        return "news/list";
    }
    
    @GetMapping("/news/{id}")
    public String viewArticle(@PathVariable Long id, Model model, Principal principal) {
        Article article = null;
        
        // Проверяем роль пользователя
        if (principal != null) {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                logger.info("Пользователь {} пытается просмотреть статью {}", user.getUsername(), id);
                logger.info("Роли пользователя: {}", user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(java.util.stream.Collectors.joining(", ")));
                
                if (user.hasRole("EDITOR") || user.hasRole("ADMIN")) {
                    logger.info("Пользователь имеет роль EDITOR или ADMIN, загружаем статью в любом статусе");
                    // Редакторы и администраторы могут видеть статьи в любом статусе
                    article = articleService.getArticleById(id).orElse(null);
                    if (article != null) {
                        logger.info("Статья найдена: {} (статус: {})", article.getTitle(), article.getStatus());
                    } else {
                        logger.warn("Статья с ID {} не найдена в базе данных", id);
                    }
                }
            }
        } else {
            logger.info("Анонимный пользователь пытается просмотреть статью {}", id);
        }
        
        // Если не редактор/админ или статья не найдена, ищем только опубликованные
        if (article == null) {
            logger.info("Загружаем только опубликованную статью с ID {}", id);
            article = articleService.getPublishedArticleById(id).orElse(null);
            if (article != null) {
                logger.info("Опубликованная статья найдена: {}", article.getTitle());
            } else {
                logger.warn("Опубликованная статья с ID {} не найдена", id);
            }
        }
        
        if (article == null) {
            logger.error("Статья с ID {} недоступна для пользователя {}", id, 
                principal != null ? principal.getName() : "anonymous");
            model.addAttribute("errorMessage", "Статья не найдена или недоступна");
            model.addAttribute("pageTitle", "Статья не найдена");
            return "error";
        }
        
        // Получаем похожие статьи из той же категории
        List<Article> relatedArticles = articleService.getRelatedArticles(article, 5);
        
        // Проверяем, сохранена ли статья текущим пользователем
        boolean isArticleSaved = false;
        if (principal != null) {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                isArticleSaved = savedArticleService.isArticleSaved(user.getId(), id);
            }
        }
        
        model.addAttribute("article", article);
        model.addAttribute("relatedArticles", relatedArticles);
        model.addAttribute("isArticleSaved", isArticleSaved);
        model.addAttribute("pageTitle", article.getTitle());
        
        return "news/view";
    }
    
    @GetMapping("/category/{id}")
    public String categoryNews(@PathVariable Long id, Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Principal principal) {
        
        Category category = categoryService.getCategoryById(id).orElse(null);
        
        if (category == null) {
            model.addAttribute("errorMessage", "Категория не найдена");
            model.addAttribute("pageTitle", "Категория не найдена");
            return "error";
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles = articleService.getPublishedArticlesByCategory(id, pageable);
        List<Category> allCategories = categoryService.getAllCategories();
        
        // Получаем информацию о сохраненных статьях для авторизованного пользователя
        if (principal != null) {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                List<Long> savedArticleIds = articles.getContent().stream()
                    .filter(article -> savedArticleService.isArticleSaved(user.getId(), article.getId()))
                    .map(Article::getId)
                    .toList();
                model.addAttribute("savedArticleIds", savedArticleIds);
            }
        }
        
        model.addAttribute("category", category);
        model.addAttribute("articles", articles);
        model.addAttribute("categories", allCategories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("pageTitle", "Категория: " + category.getName());
        
        return "news/category";
    }
    
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) Long sourceId,
                        @RequestParam(required = false) String dateFrom,
                        @RequestParam(required = false) String dateTo,
                        @RequestParam(defaultValue = "publishedAt") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDir,
                        Principal principal) {
        
        List<Category> categories = categoryService.getAllCategories();
        List<com.newsaggregator.entity.NewsSource> sources = newsSourceService.getActiveSources();
        
        model.addAttribute("categories", categories);
        model.addAttribute("sources", sources);
        model.addAttribute("searchQuery", q);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSourceId", sourceId);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        if (q == null || q.trim().isEmpty()) {
            model.addAttribute("pageTitle", "Поиск");
            return "news/search";
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles = articleService.searchPublishedArticlesWithFilters(
            q.trim(), categoryId, sourceId, dateFrom, dateTo, sortBy, sortDir, pageable);
        
        // Получаем информацию о сохраненных статьях для авторизованного пользователя
        if (principal != null) {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                List<Long> savedArticleIds = articles.getContent().stream()
                    .filter(article -> savedArticleService.isArticleSaved(user.getId(), article.getId()))
                    .map(Article::getId)
                    .toList();
                model.addAttribute("savedArticleIds", savedArticleIds);
            }
        }
        
        model.addAttribute("articles", articles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("pageTitle", "Поиск: " + q);
        
        return "news/search";
    }
    
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        
        // Генерируем капчу для формы входа
        String captchaImage = captchaService.generateCaptchaImage();
        model.addAttribute("captchaImage", captchaImage);
        model.addAttribute("pageTitle", "Вход в систему");
        return "login";
    }
    
    @GetMapping("/register")
    public String register(Model model) {
        // Генерируем новую капчу для формы регистрации
        String captchaImage = captchaService.generateCaptchaImage();
        model.addAttribute("captchaImage", captchaImage);
        model.addAttribute("pageTitle", "Регистрация");
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                              @RequestParam String email,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              @RequestParam String captcha,
                              Model model) {
        
        // Проверка капчи
        if (!captchaService.validateCaptcha(captcha)) {
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", "Неверный код с картинки");
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
        
        // Проверка совпадения паролей
        if (!password.equals(confirmPassword)) {
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", "Пароли не совпадают");
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
        
        // Проверка длины пароля
        if (password.length() < 6) {
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", "Пароль должен содержать минимум 6 символов");
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
        
        // Проверка формата username
        if (!username.matches("[a-zA-Z0-9_]+")) {
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", "Имя пользователя может содержать только латинские буквы, цифры и подчеркивание");
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
        
        // Проверка длины username
        if (username.length() < 3 || username.length() > 50) {
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", "Имя пользователя должно содержать от 3 до 50 символов");
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
        
        // Проверка email на пустое значение
        if (email == null || email.trim().isEmpty()) {
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", "Email обязателен для заполнения");
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
        
        // Проверка формата email
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!email.matches(emailRegex)) {
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", "Неверный формат email адреса");
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
        
        // Проверка длины email
        if (email.length() > 100) {
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", "Email слишком длинный (максимум 100 символов)");
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
        
        try {
            // Создаем пользователя с ролью READER
            userService.createUser(username, email, password, "READER");
            
            // Успешная регистрация - перенаправляем на страницу входа
            model.addAttribute("success", "Регистрация успешна! Теперь вы можете войти в систему.");
            return "redirect:/login?registered";
            
        } catch (IllegalArgumentException e) {
            // Обработка ошибок (пользователь уже существует и т.д.)
            String captchaImage = captchaService.generateCaptchaImage();
            model.addAttribute("captchaImage", captchaImage);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Регистрация");
            return "auth/register";
        }
    }
    
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "О проекте");
        return "about";
    }
}