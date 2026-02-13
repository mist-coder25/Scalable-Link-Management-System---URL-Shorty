package com.urlshortener.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    private UrlMapping urlMapping;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String ipAddress;
    private String country;
    private String device; // mobile, desktop
    private String browser;
    private String referrer;

    public ClickEvent() {}
    
    public ClickEvent(UrlMapping urlMapping, LocalDateTime timestamp, String ipAddress, String country, String device, String browser, String referrer) {
        this.urlMapping = urlMapping;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.country = country;
        this.device = device;
        this.browser = browser;
        this.referrer = referrer;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UrlMapping getUrlMapping() { return urlMapping; }
    public void setUrlMapping(UrlMapping urlMapping) { this.urlMapping = urlMapping; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
}
