package com.urlshortener.controller;

import com.urlshortener.model.UrlMapping;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserRepository userRepository;
    private final UrlService urlService;
    private final com.urlshortener.service.AnalyticsService analyticsService;

    public DashboardController(UserRepository userRepository, UrlService urlService, com.urlshortener.service.AnalyticsService analyticsService) {
        this.userRepository = userRepository;
        this.urlService = urlService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/user")
    public UserDto getUserDetails(Principal principal) {
        User user = getUser(principal);
        return new UserDto(user.getFullName(), user.getEmail());
    }

    @GetMapping("/links")
    public List<LinkDto> getUserLinks(Principal principal) {
        User user = getUser(principal);
        return urlService.getLinksByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/stats")
    public com.urlshortener.dto.AnalyticsResponse getGlobalStats(Principal principal) {
        User user = getUser(principal);
        List<UrlMapping> mappings = urlService.getLinksByUser(user);
        return analyticsService.getGlobalAnalytics(mappings);
    }

    private User getUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        
        String email = null;
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            email = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal).getPrincipal().getAttribute("email");
        } else {
            email = principal.getName();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
    
    private LinkDto convertToDto(UrlMapping mapping) {
        // Use relative path or configured domain
        String shortUrl = "http://localhost:8080/" + mapping.getShortCode(); 
        return new LinkDto(
                mapping.getId(),
                mapping.getOriginalUrl(),
                shortUrl,
                mapping.getShortCode(),
                mapping.getClickCount(),
                mapping.getCreatedAt().toString(),
                mapping.isActive()
        );
    }

    static class UserDto {
        private String fullName;
        private String email;

        public UserDto(String fullName, String email) {
            this.fullName = fullName;
            this.email = email;
        }

        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
    }

    static class LinkDto {
        private Long id;
        private String originalUrl;
        private String shortUrl;
        private String shortCode;
        private long clickCount;
        private String createdAt;
        private boolean active;

        public LinkDto(Long id, String originalUrl, String shortUrl, String shortCode, long clickCount, String createdAt, boolean active) {
            this.id = id;
            this.originalUrl = originalUrl;
            this.shortUrl = shortUrl;
            this.shortCode = shortCode;
            this.clickCount = clickCount;
            this.createdAt = createdAt;
            this.active = active;
        }

        public Long getId() { return id; }
        public String getOriginalUrl() { return originalUrl; }
        public String getShortUrl() { return shortUrl; }
        public String getShortCode() { return shortCode; }
        public long getClickCount() { return clickCount; }
        public String getCreatedAt() { return createdAt; }
        public boolean isActive() { return active; }
    }
}
