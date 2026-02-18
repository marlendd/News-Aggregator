package com.newsaggregator.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.newsaggregator.entity.Category;
import com.newsaggregator.service.CategoryService;

@ControllerAdvice
public class GlobalControllerAdvice {
    
    @Autowired
    private CategoryService categoryService;
    
    @ModelAttribute("categories")
    public List<Category> getCategories() {
        try {
            return categoryService.getAllCategories();
        } catch (Exception e) {
            // Return empty list if there's an error (e.g., database not initialized)
            return new ArrayList<>();
        }
    }
    
    @ModelAttribute("searchQuery")
    public String getSearchQuery() {
        return ""; // Default empty search query
    }
}