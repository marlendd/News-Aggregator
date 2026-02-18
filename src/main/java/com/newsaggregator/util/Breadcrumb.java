package com.newsaggregator.util;

public class Breadcrumb {
    private String title;
    private String url;
    
    public Breadcrumb(String title, String url) {
        this.title = title;
        this.url = url;
    }
    
    public Breadcrumb(String title) {
        this.title = title;
        this.url = null;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
}
