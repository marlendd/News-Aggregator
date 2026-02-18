package com.newsaggregator.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "news_sources")
public class NewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "rss_url", nullable = false, unique = true, length = 1000)
    private String rssUrl;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "error_count")
    private int errorCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Article> articles;

    // Конструкторы
    public NewsSource() {
        this.createdAt = LocalDateTime.now();
    }

    public NewsSource(String name, String rssUrl, String websiteUrl) {
        this();
        this.name = name;
        this.rssUrl = rssUrl;
        this.websiteUrl = websiteUrl;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRssUrl() {
        return rssUrl;
    }

    public void setRssUrl(String rssUrl) {
        this.rssUrl = rssUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Article> getArticles() {
        return articles;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }

    // Вспомогательные методы
    public boolean hasErrors() {
        return errorCount > 0;
    }

    public String getStatusText() {
        if (!active) {
            return "Отключен";
        } else if (hasErrors()) {
            return "Ошибки (" + errorCount + ")";
        } else {
            return "Активен";
        }
    }

    @Override
    public String toString() {
        return "NewsSource{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", rssUrl='" + rssUrl + '\'' +
                ", active=" + active +
                ", errorCount=" + errorCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NewsSource)) return false;
        NewsSource that = (NewsSource) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}