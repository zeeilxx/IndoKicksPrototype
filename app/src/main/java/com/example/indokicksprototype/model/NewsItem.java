package com.example.indokicksprototype.model;

public class NewsItem {
    private String title;
    private String summary;
    private String imageUrl;
    public NewsItem(String title, String summary, String imageUrl) {
        this.title = title; this.summary = summary; this.imageUrl = imageUrl;
    }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getImageUrl() { return imageUrl; }
}
