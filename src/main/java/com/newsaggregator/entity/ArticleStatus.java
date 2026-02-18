package com.newsaggregator.entity;

public enum ArticleStatus {
    PENDING("На модерации"),
    PUBLISHED("Опубликована"),
    REJECTED("Отклонена"),
    DRAFT("Черновик");
    
    private final String displayName;
    
    ArticleStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}