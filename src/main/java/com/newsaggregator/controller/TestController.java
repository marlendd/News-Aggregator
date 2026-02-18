package com.newsaggregator.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.newsaggregator.service.LMStudioService;

@Controller
@RequestMapping("/test")
@PreAuthorize("hasRole('ADMIN')")
public class TestController {

    @Autowired
    private LMStudioService lmStudioService;

    /**
     * Страница тестирования ИИ
     */
    @GetMapping("/ai")
    public String testAiPage(Model model) {
        model.addAttribute("configured", lmStudioService.isConfigured());
        model.addAttribute("available", lmStudioService.isApiAvailable());
        
        // Получаем список доступных моделей
        try {
            List<String> models = lmStudioService.getAvailableModels();
            model.addAttribute("models", models);
        } catch (Exception e) {
            model.addAttribute("models", List.of());
        }
        
        return "test/ai";
    }

    /**
     * Тестирование категоризации
     */
    @PostMapping("/categorize")
    public String testCategorization(@RequestParam String title, 
                                   @RequestParam String content, 
                                   Model model) {
        try {
            String category = lmStudioService.categorizeArticle(title, content);
            model.addAttribute("result", "Категория: " + category);
            model.addAttribute("success", true);
        } catch (Exception e) {
            model.addAttribute("result", "Ошибка: " + e.getMessage());
            model.addAttribute("success", false);
        }
        
        model.addAttribute("title", title);
        model.addAttribute("content", content);
        model.addAttribute("configured", lmStudioService.isConfigured());
        model.addAttribute("available", lmStudioService.isApiAvailable());
        return "test/ai";
    }

    /**
     * Тестирование генерации сводки
     */
    @PostMapping("/summarize")
    public String testSummarization(@RequestParam String content, Model model) {
        try {
            String summary = lmStudioService.generateSummary(content);
            model.addAttribute("result", "Сводка: " + summary);
            model.addAttribute("success", true);
        } catch (Exception e) {
            model.addAttribute("result", "Ошибка: " + e.getMessage());
            model.addAttribute("success", false);
        }
        
        model.addAttribute("content", content);
        model.addAttribute("configured", lmStudioService.isConfigured());
        model.addAttribute("available", lmStudioService.isApiAvailable());
        return "test/ai";
    }
}