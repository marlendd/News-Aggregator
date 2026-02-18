package com.newsaggregator.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.newsaggregator.entity.Article;
import com.newsaggregator.entity.User;
import com.newsaggregator.service.SavedArticleService;
import com.newsaggregator.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SavedArticleService savedArticleService;
    
    @Autowired
    private com.newsaggregator.service.CategoryService categoryService;
    
    @Autowired
    private com.newsaggregator.service.NewsSourceService newsSourceService;
    
    @Autowired
    private com.newsaggregator.repository.UserPreferencesRepository userPreferencesRepository;
    
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        // Получаем количество сохраненных статей
        long savedArticlesCount = savedArticleService.getSavedArticlesCount(user.getId());
        
        // Хлебные крошки
        List<com.newsaggregator.util.Breadcrumb> breadcrumbs = new java.util.ArrayList<>();
        breadcrumbs.add(new com.newsaggregator.util.Breadcrumb("Профиль"));
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        model.addAttribute("user", user);
        model.addAttribute("savedArticlesCount", savedArticlesCount);
        model.addAttribute("pageTitle", "Профиль пользователя");
        
        return "user/profile";
    }
    
    @GetMapping("/saved")
    @PreAuthorize("isAuthenticated()")
    public String savedArticles(Principal principal, Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size) {
        
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> savedArticles = savedArticleService.getSavedArticles(user.getId(), pageable);
        
        // Хлебные крошки
        List<com.newsaggregator.util.Breadcrumb> breadcrumbs = new java.util.ArrayList<>();
        breadcrumbs.add(new com.newsaggregator.util.Breadcrumb("Профиль", "/user/profile"));
        breadcrumbs.add(new com.newsaggregator.util.Breadcrumb("Сохраненные статьи"));
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        model.addAttribute("articles", savedArticles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", savedArticles.getTotalPages());
        model.addAttribute("pageTitle", "Сохраненные статьи");
        
        return "user/saved";
    }
    
    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public String preferences(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        // Явно загружаем настройки пользователя из репозитория
        com.newsaggregator.entity.UserPreferences preferences = userPreferencesRepository.findByUserId(user.getId())
                .orElse(new com.newsaggregator.entity.UserPreferences(user));
        
        // Хлебные крошки
        List<com.newsaggregator.util.Breadcrumb> breadcrumbs = new java.util.ArrayList<>();
        breadcrumbs.add(new com.newsaggregator.util.Breadcrumb("Профиль", "/user/profile"));
        breadcrumbs.add(new com.newsaggregator.util.Breadcrumb("Настройки"));
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        model.addAttribute("user", user);
        model.addAttribute("preferences", preferences);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("sources", newsSourceService.getActiveSources());
        model.addAttribute("pageTitle", "Настройки");
        
        return "user/preferences";
    }
    
    @PostMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public String savePreferences(@RequestParam(defaultValue = "10") int articlesPerPage,
                                 @RequestParam(required = false) java.util.List<Long> subscribedCategories,
                                 @RequestParam(required = false) java.util.List<Long> preferredSources,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        try {
            // Явно загружаем настройки пользователя из репозитория
            com.newsaggregator.entity.UserPreferences preferences = userPreferencesRepository.findByUserId(user.getId())
                    .orElse(new com.newsaggregator.entity.UserPreferences(user));
            
            // Сохраняем количество статей на странице
            preferences.setArticlesPerPage(articlesPerPage);
            
            // Сохраняем выбранные категории в формате JSON
            if (subscribedCategories != null && !subscribedCategories.isEmpty()) {
                String categoriesJson = subscribedCategories.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
                preferences.setSubscribedCategories(categoriesJson);
            } else {
                preferences.setSubscribedCategories(null);
            }
            
            // Сохраняем выбранные источники в формате JSON
            if (preferredSources != null && !preferredSources.isEmpty()) {
                String sourcesJson = preferredSources.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
                preferences.setPreferredSources(sourcesJson);
            } else {
                preferences.setPreferredSources(null);
            }
            
            // Сохраняем настройки через репозиторий
            user.setPreferences(preferences);
            userService.saveUserPreferences(user, preferences);
            
            redirectAttributes.addFlashAttribute("successMessage", "Настройки успешно сохранены!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при сохранении настроек: " + e.getMessage());
        }
        
        return "redirect:/user/preferences";
    }
    
    /**
     * Сохранить статью
     */
    @PostMapping("/save-article")
    @PreAuthorize("isAuthenticated()")
    public String saveArticle(@RequestParam Long articleId, 
                             @RequestParam(required = false) String returnUrl,
                             Principal principal, 
                             RedirectAttributes redirectAttributes) {
        
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        boolean success = savedArticleService.saveArticle(user.getId(), articleId);
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Статья сохранена!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось сохранить статью");
        }
        
        // Возвращаемся на предыдущую страницу или на главную
        return "redirect:" + (returnUrl != null ? returnUrl : "/");
    }
    
    /**
     * Удалить статью из сохраненных
     */
    @PostMapping("/unsave-article")
    @PreAuthorize("isAuthenticated()")
    public String unsaveArticle(@RequestParam Long articleId, 
                               @RequestParam(required = false) String returnUrl,
                               Principal principal, 
                               RedirectAttributes redirectAttributes) {
        
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        boolean success = savedArticleService.unsaveArticle(user.getId(), articleId);
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Статья удалена из сохраненных!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось удалить статью");
        }
        
        // Возвращаемся на предыдущую страницу или на главную
        return "redirect:" + (returnUrl != null ? returnUrl : "/");
    }
}