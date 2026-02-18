package com.newsaggregator.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "saved_articles", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "article_id"}))
public class SavedArticle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;
    
    // Конструкторы
    public SavedArticle() {
        this.savedAt = LocalDateTime.now();
    }
    
    public SavedArticle(User user, Article article) {
        this();
        this.user = user;
        this.article = article;
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Article getArticle() {
        return article;
    }
    
    public void setArticle(Article article) {
        this.article = article;
    }
    
    public LocalDateTime getSavedAt() {
        return savedAt;
    }
    
    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }
    
    // equals и hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedArticle)) return false;
        
        SavedArticle that = (SavedArticle) o;
        
        if (user != null ? !user.getId().equals(that.user != null ? that.user.getId() : null) : that.user != null) return false;
        return article != null ? article.getId().equals(that.article != null ? that.article.getId() : null) : that.article == null;
    }
    
    @Override
    public int hashCode() {
        int result = user != null ? user.getId().hashCode() : 0;
        result = 31 * result + (article != null ? article.getId().hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "SavedArticle{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", articleId=" + (article != null ? article.getId() : null) +
                ", savedAt=" + savedAt +
                '}';
    }
}