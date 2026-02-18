package com.newsaggregator.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "subscribed_categories", columnDefinition = "TEXT")
    private String subscribedCategories; // JSON array of category IDs
    
    @Column(name = "preferred_sources", columnDefinition = "TEXT")
    private String preferredSources; // JSON array of source IDs
    
    @Column(name = "email_notifications")
    private boolean emailNotifications = false;
    
    @Column(name = "articles_per_page")
    private int articlesPerPage = 10;
    
    @Column(name = "theme")
    private String theme = "light"; // light, dark
    
    @Column(name = "language")
    private String language = "ru"; // ru, en
    
    // Constructors
    public UserPreferences() {}
    
    public UserPreferences(User user) {
        this.user = user;
    }
    
    // Helper methods
    public boolean hasSubscribedCategories() {
        return subscribedCategories != null && !subscribedCategories.trim().isEmpty();
    }
    
    public boolean hasPreferredSources() {
        return preferredSources != null && !preferredSources.trim().isEmpty();
    }
    
    public boolean isDarkTheme() {
        return "dark".equals(theme);
    }
    
    public boolean isLightTheme() {
        return "light".equals(theme);
    }
    
    public java.util.List<Long> getSubscribedCategoryIds() {
        if (!hasSubscribedCategories()) {
            return new java.util.ArrayList<>();
        }
        try {
            String cleanJson = subscribedCategories.replaceAll("[\\[\\]]", "");
            if (cleanJson.trim().isEmpty()) {
                return new java.util.ArrayList<>();
            }
            return java.util.Arrays.stream(cleanJson.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    public java.util.List<Long> getPreferredSourceIds() {
        if (!hasPreferredSources()) {
            return new java.util.ArrayList<>();
        }
        try {
            String cleanJson = preferredSources.replaceAll("[\\[\\]]", "");
            if (cleanJson.trim().isEmpty()) {
                return new java.util.ArrayList<>();
            }
            return java.util.Arrays.stream(cleanJson.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    public boolean isCategorySubscribed(Long categoryId) {
        return getSubscribedCategoryIds().contains(categoryId);
    }
    
    public boolean isSourcePreferred(Long sourceId) {
        return getPreferredSourceIds().contains(sourceId);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getSubscribedCategories() { return subscribedCategories; }
    public void setSubscribedCategories(String subscribedCategories) { 
        this.subscribedCategories = subscribedCategories; 
    }
    
    public String getPreferredSources() { return preferredSources; }
    public void setPreferredSources(String preferredSources) { 
        this.preferredSources = preferredSources; 
    }
    
    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { 
        this.emailNotifications = emailNotifications; 
    }
    
    public int getArticlesPerPage() { return articlesPerPage; }
    public void setArticlesPerPage(int articlesPerPage) { 
        this.articlesPerPage = articlesPerPage; 
    }
    
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreferences that = (UserPreferences) o;
        return Objects.equals(id, that.id) && Objects.equals(user, that.user);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, user);
    }
    
    @Override
    public String toString() {
        return "UserPreferences{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", emailNotifications=" + emailNotifications +
                ", articlesPerPage=" + articlesPerPage +
                ", theme='" + theme + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}