package com.urlshortener.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_mapping", indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
})
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "url_id_seq")
    @SequenceGenerator(name = "url_id_seq", sequenceName = "url_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 50)
    private String shortCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(nullable = false)
    private Long clickCount = 0L;

    @Column(nullable = false)
    private boolean isActive = true;

    private Long maxClicks;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password; // Hashed password

    @Column(columnDefinition = "TEXT")
    private String geoRules; // JSON: {"US": "http://target", "IN": "http://target2"}

    @Column(columnDefinition = "TEXT")
    private String deviceRules; // JSON: {"mobile": "http://app", "desktop": "http://web"}

    public UrlMapping() {}

    public UrlMapping(String originalUrl, String shortCode, LocalDateTime createdAt, LocalDateTime expiresAt, User user, 
                     Long clickCount, boolean isActive, Long maxClicks, String password, String geoRules, String deviceRules) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.user = user;
        this.clickCount = clickCount;
        this.isActive = isActive;
        this.maxClicks = maxClicks;
        this.password = password;
        this.geoRules = geoRules;
        this.deviceRules = deviceRules;
    }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClickCount() { return clickCount; }
    public void setClickCount(Long clickCount) { this.clickCount = clickCount; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Long getMaxClicks() { return maxClicks; }
    public void setMaxClicks(Long maxClicks) { this.maxClicks = maxClicks; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getGeoRules() { return geoRules; }
    public void setGeoRules(String geoRules) { this.geoRules = geoRules; }
    public String getDeviceRules() { return deviceRules; }
    public void setDeviceRules(String deviceRules) { this.deviceRules = deviceRules; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}

