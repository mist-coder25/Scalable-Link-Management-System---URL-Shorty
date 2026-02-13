package com.urlshortener.dto;

import java.time.LocalDateTime;

public class UrlResponseDto {
    private Long id;
    private String originalUrl;
    private String shortCode;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long clickCount;
    private boolean isActive;
    
    public UrlResponseDto(Long id, String originalUrl, String shortCode, LocalDateTime createdAt, LocalDateTime expiresAt, Long clickCount, boolean isActive) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.isActive = isActive;
    }

    // Getters
    public Long getId() { return id; }
    public String getOriginalUrl() { return originalUrl; }
    public String getShortCode() { return shortCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Long getClickCount() { return clickCount; }
    public boolean isActive() { return isActive; }
    
    // Setters
    public void setId(Long id) { this.id = id; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setClickCount(Long clickCount) { this.clickCount = clickCount; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
}
