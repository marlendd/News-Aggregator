package com.newsaggregator.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.ArticleStatus;
import com.newsaggregator.entity.Category;
import com.newsaggregator.entity.NewsSource;
import com.newsaggregator.entity.User;
import com.newsaggregator.service.ArticleService;
import com.newsaggregator.service.CategoryService;
import com.newsaggregator.service.NewsSourceService;
import com.newsaggregator.service.RssParserService;
import com.newsaggregator.service.UserService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private NewsSourceService newsSourceService;
    
    @Autowired
    private RssParserService rssParserService;
    
    @GetMapping
    public String dashboard(Model model) {
        return adminDashboard(model);
    }
    
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        // Статистика для дашборда
        long totalUsers = userService.getTotalUsersCount();
        long totalArticles = articleService.getTotalArticlesCount();
        long publishedArticles = articleService.getPublishedArticlesCount();
        long pendingArticles = articleService.getPendingArticlesCount();
        long totalCategories = categoryService.getTotalCategoriesCount();
        long totalSources = newsSourceService.getTotalSourcesCount();
        long activeSources = newsSourceService.getActiveSourcesCount();
        
        // Последние пользователи
        List<User> recentUsers = userService.getRecentActiveUsers();
        
        // Последние статьи
        Pageable recentArticlesPageable = PageRequest.of(0, 5);
        Page<Article> recentArticles = articleService.getAllArticles(recentArticlesPageable);
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalArticles", totalArticles);
        model.addAttribute("publishedArticles", publishedArticles);
        model.addAttribute("pendingArticles", pendingArticles);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalSources", totalSources);
        model.addAttribute("activeSources", activeSources);
        model.addAttribute("recentUsers", recentUsers);
        model.addAttribute("recentArticles", recentArticles.getContent());
        model.addAttribute("pageTitle", "Панель администратора");
        
        return "admin/dashboard";
    }
    
    // === УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ ===
    
    @GetMapping("/users")
    public String users(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;
        
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search.trim(), pageable);
            model.addAttribute("search", search);
        } else {
            users = userService.findAll(pageable);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("pageTitle", "Управление пользователями");
        
        return "admin/users";
    }
    
    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, 
                                   RedirectAttributes redirectAttributes,
                                   java.security.Principal principal) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            
            // Проверка: админ не может заблокировать самого себя
            if (principal != null && user.getUsername().equals(principal.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Вы не можете заблокировать самого себя");
                return "redirect:/admin/users";
            }
            
            // Проверка: нельзя деактивировать последнего администратора
            if (user.isEnabled() && userService.isLastAdmin(user)) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Нельзя деактивировать последнего администратора в системе");
                return "redirect:/admin/users";
            }
            
            if (user.isEnabled()) {
                userService.deactivateUser(id);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Пользователь " + user.getUsername() + " заблокирован");
            } else {
                userService.activateUser(id);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Пользователь " + user.getUsername() + " разблокирован");
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/change-role")
    public String changeUserRole(@PathVariable Long id, 
                                @RequestParam String newRole,
                                RedirectAttributes redirectAttributes,
                                java.security.Principal principal) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            
            // Проверка: админ не может изменить свою роль
            if (principal != null && user.getUsername().equals(principal.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Вы не можете изменить свою собственную роль");
                return "redirect:/admin/users";
            }
            
            // Проверка: нельзя изменить роль на пустую или недопустимую
            if (newRole == null || newRole.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Роль не может быть пустой");
                return "redirect:/admin/users";
            }
            
            // Проверка: роль должна быть одной из допустимых
            if (!newRole.equals("READER") && !newRole.equals("EDITOR") && !newRole.equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Недопустимая роль: " + newRole);
                return "redirect:/admin/users";
            }
            
            // Проверка: нельзя изменить роль последнего администратора
            if (!userService.canChangeUserRole(id, newRole)) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Нельзя изменить роль последнего администратора в системе");
                return "redirect:/admin/users";
            }
            
            userService.changeUserRole(id, newRole);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Роль пользователя " + user.getUsername() + " изменена на " + newRole);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    // === УПРАВЛЕНИЕ КАТЕГОРИЯМИ ===
    
    @GetMapping("/categories")
    public String categories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Управление категориями");
        return "admin/categories";
    }
    
    @PostMapping("/categories")
    public String createCategory(@RequestParam String name,
                                @RequestParam String description,
                                @RequestParam String color,
                                RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(name, description, color);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Категория '" + name + "' успешно создана");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/categories/{id}/update")
    public String updateCategory(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam String description,
                                @RequestParam String color,
                                RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, name, description, color);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Категория '" + name + "' успешно обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Категория не найдена"));
            
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Категория '" + category.getName() + "' успешно удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }
    
    // === УПРАВЛЕНИЕ СТАТЬЯМИ ===
    
    @GetMapping("/articles")
    public String articles(Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String status) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles;
        
        if (status != null && !status.isEmpty()) {
            ArticleStatus articleStatus = ArticleStatus.valueOf(status);
            articles = articleService.getArticlesByStatus(articleStatus, pageable);
            model.addAttribute("selectedStatus", status);
        } else {
            articles = articleService.getAllArticles(pageable);
        }
        
        model.addAttribute("articles", articles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("pageTitle", "Управление статьями");
        
        return "admin/articles";
    }
    
    @PostMapping("/articles/{id}/delete")
    public String deleteArticle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Article article = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Статья не найдена"));
            
            articleService.deleteArticle(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Статья '" + article.getTitle() + "' успешно удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/articles";
    }
    
    @PostMapping("/articles/reject-all")
    public String rejectAllPendingArticles(RedirectAttributes redirectAttributes) {
        try {
            int rejectedCount = articleService.rejectAllPendingArticles();
            redirectAttributes.addFlashAttribute("successMessage", 
                "Отклонено " + rejectedCount + " статей");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/articles?status=PENDING";
    }
    
    @PostMapping("/articles/delete-rejected")
    public String deleteAllRejectedArticles(RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = articleService.deleteAllRejectedArticles();
            redirectAttributes.addFlashAttribute("successMessage", 
                "Удалено " + deletedCount + " отклоненных статей");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/articles";
    }
    
    @PostMapping("/articles/reject-all-articles")
    public String rejectAllArticles(RedirectAttributes redirectAttributes) {
        try {
            int rejectedCount = articleService.rejectAllArticles();
            redirectAttributes.addFlashAttribute("successMessage", 
                "Отклонено " + rejectedCount + " статей");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/articles";
    }
    
    @PostMapping("/articles/delete-published")
    public String deleteAllPublishedArticles(RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = articleService.deleteAllPublishedArticles();
            redirectAttributes.addFlashAttribute("successMessage", 
                "Удалено " + deletedCount + " опубликованных статей");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/articles";
    }
    
    @PostMapping("/articles/delete-all")
    public String deleteAllArticles(RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = articleService.deleteAllArticles();
            redirectAttributes.addFlashAttribute("successMessage", 
                "Удалено " + deletedCount + " статей");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/articles";
    }
    
    // === УПРАВЛЕНИЕ RSS ИСТОЧНИКАМИ ===
    
    @GetMapping("/sources")
    public String sources(Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NewsSource> sources;
        
        if (search != null && !search.trim().isEmpty()) {
            sources = newsSourceService.searchSources(search.trim(), pageable);
            model.addAttribute("search", search);
        } else {
            sources = newsSourceService.getAllSources(pageable);
        }
        
        model.addAttribute("sources", sources);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sources.getTotalPages());
        model.addAttribute("pageTitle", "Управление RSS источниками");
        
        return "admin/sources";
    }
    
    @PostMapping("/sources")
    public String createSource(@RequestParam String name,
                              @RequestParam String rssUrl,
                              @RequestParam(required = false) String websiteUrl,
                              RedirectAttributes redirectAttributes) {
        try {
            newsSourceService.createSource(name, rssUrl, websiteUrl);
            redirectAttributes.addFlashAttribute("successMessage", 
                "RSS источник '" + name + "' успешно создан");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/{id}/update")
    public String updateSource(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam String rssUrl,
                              @RequestParam(required = false) String websiteUrl,
                              @RequestParam(defaultValue = "false") boolean active,
                              RedirectAttributes redirectAttributes) {
        try {
            newsSourceService.updateSource(id, name, rssUrl, websiteUrl, active);
            redirectAttributes.addFlashAttribute("successMessage", 
                "RSS источник '" + name + "' успешно обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/{id}/toggle-status")
    public String toggleSourceStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            NewsSource source = newsSourceService.getSourceById(id)
                    .orElseThrow(() -> new RuntimeException("Источник не найден"));
            
            // Сохраняем старый статус ДО изменения
            boolean wasActive = source.isActive();
            
            newsSourceService.toggleSourceStatus(id);
            
            // Определяем новый статус на основе старого
            String status = wasActive ? "отключен" : "активирован";
            redirectAttributes.addFlashAttribute("successMessage", 
                "Источник '" + source.getName() + "' " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/{id}/reset-errors")
    public String resetSourceErrors(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            NewsSource source = newsSourceService.getSourceById(id)
                    .orElseThrow(() -> new RuntimeException("Источник не найден"));
            
            newsSourceService.resetSourceErrors(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Ошибки источника '" + source.getName() + "' сброшены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/{id}/delete")
    public String deleteSource(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            NewsSource source = newsSourceService.getSourceById(id)
                    .orElseThrow(() -> new RuntimeException("Источник не найден"));
            
            newsSourceService.deleteSource(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "RSS источник '" + source.getName() + "' успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/{id}/parse")
    public String parseSource(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            NewsSource source = newsSourceService.getSourceById(id)
                    .orElseThrow(() -> new RuntimeException("Источник не найден"));
            
            rssParserService.parseRssFeed(source);
            redirectAttributes.addFlashAttribute("successMessage", 
                "RSS лента '" + source.getName() + "' успешно обработана");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка при парсинге '" + e.getMessage() + "'");
        }
        
        return "redirect:/admin/sources";
    }
    
    @PostMapping("/sources/parse-all")
    public String parseAllSources(RedirectAttributes redirectAttributes) {
        try {
            rssParserService.parseAllRssFeeds();
            redirectAttributes.addFlashAttribute("successMessage", 
                "Все RSS ленты успешно обработаны");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка при парсинге RSS лент: " + e.getMessage());
        }
        
        return "redirect:/admin/sources";
    }
}
