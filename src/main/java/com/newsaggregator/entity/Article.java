package com.newsaggregator.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "articles")
public class Article {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "source_url", unique = true, nullable = false, length = 1000)
    private String sourceUrl;
    
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleStatus status = ArticleStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private NewsSource source;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SavedArticle> savedByEntries = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Article() {}
    
    public Article(String title, String content, String sourceUrl, LocalDateTime publishedAt) {
        this.title = title;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.publishedAt = publishedAt;
    }
    
    // Helper methods
    public boolean isPublished() {
        return status == ArticleStatus.PUBLISHED;
    }
    
    public boolean isPending() {
        return status == ArticleStatus.PENDING;
    }
    
    public boolean isRejected() {
        return status == ArticleStatus.REJECTED;
    }
    
    public boolean isDraft() {
        return status == ArticleStatus.DRAFT;
    }
    
    public void publish() {
        this.status = ArticleStatus.PUBLISHED;
    }
    
    public void reject() {
        this.status = ArticleStatus.REJECTED;
    }
    
    public void makeDraft() {
        this.status = ArticleStatus.DRAFT;
    }
    
    public String getShortContent(int maxLength) {
        if (content == null) return "";
        return content.length() > maxLength ? 
            content.substring(0, maxLength) + "..." : content;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    
    public ArticleStatus getStatus() { return status; }
    public void setStatus(ArticleStatus status) { this.status = status; }
    
    public NewsSource getSource() { return source; }
    public void setSource(NewsSource source) { this.source = source; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public Set<SavedArticle> getSavedByEntries() { return savedByEntries; }
    public void setSavedByEntries(Set<SavedArticle> savedByEntries) { this.savedByEntries = savedByEntries; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return Objects.equals(id, article.id) && Objects.equals(sourceUrl, article.sourceUrl);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, sourceUrl);
    }
    
    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", status=" + status +
                ", publishedAt=" + publishedAt +
                '}';
    }
}