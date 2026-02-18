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
import com.newsaggregator.service.ArticleService;
import com.newsaggregator.service.CategoryService;

@Controller
@RequestMapping("/editor")
@PreAuthorize("hasRole('EDITOR') or hasRole('ADMIN')")
public class EditorController {
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private com.newsaggregator.service.NewsSourceService newsSourceService;
    
    @GetMapping
    public String dashboard(Model model) {
        // Статистика для редактора
        long totalArticles = articleService.getTotalArticlesCount();
        long publishedArticles = articleService.getPublishedArticlesCount();
        long pendingArticles = articleService.getPendingArticlesCount();
        
        // Статьи на модерации
        Pageable pendingPageable = PageRequest.of(0, 10);
        Page<Article> pendingArticlesList = articleService.getArticlesByStatus(ArticleStatus.PENDING, pendingPageable);
        
        // Последние опубликованные статьи
        Pageable recentPageable = PageRequest.of(0, 5);
        Page<Article> recentPublished = articleService.getArticlesByStatus(ArticleStatus.PUBLISHED, recentPageable);
        
        model.addAttribute("totalArticles", totalArticles);
        model.addAttribute("publishedArticles", publishedArticles);
        model.addAttribute("pendingArticles", pendingArticles);
        model.addAttribute("pendingArticlesList", pendingArticlesList.getContent());
        model.addAttribute("recentPublished", recentPublished.getContent());
        model.addAttribute("pageTitle", "Панель редактора");
        
        return "editor/dashboard";
    }
    
    // === МОДЕРАЦИЯ СТАТЕЙ ===
    
    @GetMapping("/moderate")
    public String moderateArticles(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) Long sourceId) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles;
        
        ArticleStatus articleStatus = ArticleStatus.PENDING; // По умолчанию
        if (status != null && !status.isEmpty()) {
            articleStatus = ArticleStatus.valueOf(status);
            model.addAttribute("selectedStatus", status);
        } else {
            model.addAttribute("selectedStatus", "PENDING");
        }
        
        // Фильтрация по статусу, категории и источнику
        if (categoryId != null && sourceId != null) {
            // Фильтр по категории и источнику
            articles = articleService.getArticlesByStatusCategoryAndSource(articleStatus, categoryId, sourceId, pageable);
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("selectedSourceId", sourceId);
        } else if (categoryId != null) {
            // Только по категории
            articles = articleService.getArticlesByStatusAndCategory(articleStatus, categoryId, pageable);
            model.addAttribute("selectedCategoryId", categoryId);
        } else if (sourceId != null) {
            // Только по источнику
            articles = articleService.getArticlesByStatusAndSource(articleStatus, sourceId, pageable);
            model.addAttribute("selectedSourceId", sourceId);
        } else {
            // Только по статусу
            articles = articleService.getArticlesByStatus(articleStatus, pageable);
        }
        
        List<Category> categories = categoryService.getAllCategories();
        List<com.newsaggregator.entity.NewsSource> sources = newsSourceService.getActiveSources();
        
        model.addAttribute("articles", articles);
        model.addAttribute("categories", categories);
        model.addAttribute("sources", sources);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("pageTitle", "Модерация статей");
        
        return "editor/moderate";
    }
    
    @PostMapping("/articles/{id}/approve")
    public String approveArticle(@PathVariable Long id, 
                                @RequestParam(required = false) Long categoryId,
                                RedirectAttributes redirectAttributes) {
        try {
            Article article = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Статья не найдена"));
            
            // Устанавливаем категорию, если указана
            if (categoryId != null) {
                Category category = categoryService.getCategoryById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Категория не найдена"));
                articleService.setArticleCategory(id, category);
            }
            
            // Публикуем статью
            articleService.publishArticle(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Статья '" + article.getTitle() + "' успешно одобрена и опубликована");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/editor/moderate";
    }
    
    @PostMapping("/articles/{id}/reject")
    public String rejectArticle(@PathVariable Long id, 
                               @RequestParam(required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            Article article = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Статья не найдена"));
            
            articleService.rejectArticle(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Статья '" + article.getTitle() + "' отклонена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/editor/moderate";
    }
    
    @PostMapping("/articles/reject-all")
    public String rejectAllPendingArticles(@RequestParam(required = false) Long categoryId,
                                          RedirectAttributes redirectAttributes) {
        try {
            int rejectedCount;
            
            if (categoryId != null) {
                // Отклоняем все статьи на модерации в указанной категории
                rejectedCount = articleService.rejectAllPendingArticlesByCategory(categoryId);
                Category category = categoryService.getCategoryById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Категория не найдена"));
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Отклонено " + rejectedCount + " статей в категории '" + category.getName() + "'");
            } else {
                // Отклоняем все статьи на модерации
                rejectedCount = articleService.rejectAllPendingArticles();
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Отклонено " + rejectedCount + " статей");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/editor/moderate";
    }
    
    @PostMapping("/articles/{id}/set-category")
    public String setArticleCategory(@PathVariable Long id, 
                                    @RequestParam Long categoryId,
                                    RedirectAttributes redirectAttributes) {
        try {
            Article article = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Статья не найдена"));
            
            Category category = categoryService.getCategoryById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Категория не найдена"));
            
            articleService.setArticleCategory(id, category);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Категория '" + category.getName() + "' установлена для статьи '" + article.getTitle() + "'");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/editor/moderate";
    }
    
    // === РЕДАКТИРОВАНИЕ СТАТЕЙ ===
    
    @GetMapping("/articles/{id}/edit")
    public String editArticle(@PathVariable Long id, Model model) {
        Article article = articleService.getArticleById(id)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));
        
        List<Category> categories = categoryService.getAllCategories();
        
        model.addAttribute("article", article);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Редактирование статьи");
        
        return "editor/edit-article";
    }
    
    @PostMapping("/articles/{id}/update")
    public String updateArticle(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam String content,
                               @RequestParam(required = false) String summary,
                               @RequestParam(required = false) String imageUrl,
                               @RequestParam(required = false) Long categoryId,
                               RedirectAttributes redirectAttributes) {
        try {
            // Обновляем основную информацию статьи
            articleService.updateArticle(id, title, content, summary, imageUrl);
            
            // Устанавливаем категорию, если указана
            if (categoryId != null) {
                Category category = categoryService.getCategoryById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Категория не найдена"));
                articleService.setArticleCategory(id, category);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Статья '" + title + "' успешно обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        
        return "redirect:/editor/moderate";
    }
    
    @PostMapping("/articles/{id}/delete")
    public String deleteArticle(@PathVariable Long id, 
                                @RequestParam(required = false) String returnUrl,
                                RedirectAttributes redirectAttributes) {
        try {
            Article article = articleService.getArticleById(id)
                    .orElseThrow(() -> new RuntimeException("Статья не найдена"));
            
            String articleTitle = article.getTitle();
            articleService.deleteArticle(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Статья '" + articleTitle + "' успешно удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении: " + e.getMessage());
        }
        
        // Если указан returnUrl, редиректим туда, иначе на список новостей
        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/news";
    }
    
    // === СТАТИСТИКА ===
    
    @GetMapping("/stats")
    public String statistics(Model model) {
        // Общая статистика
        long totalArticles = articleService.getTotalArticlesCount();
        long publishedArticles = articleService.getPublishedArticlesCount();
        long pendingArticles = articleService.getPendingArticlesCount();
        long rejectedArticles = articleService.getArticlesByStatus(ArticleStatus.REJECTED, PageRequest.of(0, 1)).getTotalElements();
        
        // Статистика по категориям
        List<Category> categories = categoryService.getAllCategories();
        
        model.addAttribute("totalArticles", totalArticles);
        model.addAttribute("publishedArticles", publishedArticles);
        model.addAttribute("pendingArticles", pendingArticles);
        model.addAttribute("rejectedArticles", rejectedArticles);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Статистика редактора");
        
        return "editor/stats";
    }
}