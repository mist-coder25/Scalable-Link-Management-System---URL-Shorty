package com.urlshortener.controller;

import com.urlshortener.model.UrlMapping;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.urlshortener.service.RedirectService;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.dto.DetailedStatsResponse;
import com.urlshortener.dto.AnalyticsResponse;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final RateLimiterService rateLimiterService;
    private final com.urlshortener.repository.UserRepository userRepository;
    private final RedirectService redirectService;
    private final AnalyticsService analyticsService;

    public UrlController(UrlService urlService, RateLimiterService rateLimiterService, 
                        com.urlshortener.repository.UserRepository userRepository,
                        RedirectService redirectService, AnalyticsService analyticsService) {
        this.urlService = urlService;
        this.rateLimiterService = rateLimiterService;
        this.userRepository = userRepository;
        this.redirectService = redirectService;
        this.analyticsService = analyticsService;
        System.out.println("UrlController initialized - VERSION 2.0 (DTO Fix Applied)");
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UrlController.class);

    @PostMapping("/api/shorten")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<com.urlshortener.dto.UrlResponseDto> shorten(@RequestBody ShortenRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        
        // Step 2: Fix Current User Extraction
        com.urlshortener.model.User user = getCurrentUser();
        
        if (user == null) {
            // Guest Limiting: 5 per hour
            if (!rateLimiterService.isGuestAllowed(clientIp)) {
                 log.warn("Guest rate limit exceeded for IP: {}", clientIp);
                 throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Guest Limit: You can only create 5 links per hour. Please login for more.");
            }
        } else {
            // Standard DOS Protection
            if (!rateLimiterService.isAllowed(clientIp)) {
                 log.warn("User rate limit exceeded for IP: {}", clientIp);
                 throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
            }
        }

        try {
            UrlMapping result = urlService.shortenUrl(request, user);
            log.info("Shorten successful. ShortCode: {}", result.getShortCode());
            
            // Map to DTO
            com.urlshortener.dto.UrlResponseDto response = new com.urlshortener.dto.UrlResponseDto(
                result.getId(),
                result.getOriginalUrl(),
                result.getShortCode(),
                result.getCreatedAt(),
                result.getExpiresAt(),
                result.getClickCount(),
                result.isActive()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error shortening URL", e);
            throw e;
        }
    }
    
    private com.urlshortener.model.User getCurrentUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        
        // Extract email explicitly
        String email = null;
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            email = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
        } else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            email = ((org.springframework.security.oauth2.core.user.OAuth2User) auth.getPrincipal()).getAttribute("email");
        } else if (auth.getPrincipal() instanceof String) {
            email = (String) auth.getPrincipal();
        } else if (auth.getPrincipal() instanceof java.security.Principal) {
            email = ((java.security.Principal) auth.getPrincipal()).getName();
        }
        
        if (email == null) return null;
        
        return userRepository.findByEmail(email).orElse(null);
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, 
                                         @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                         @RequestParam(value = "pw", required = false) String password,
                                         HttpServletRequest request) {
        
        UrlMapping mapping = urlService.getStats(shortCode); // Fetch full mapping

        // 1. Basic Checks
        if (!mapping.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This link has been disabled");
        }
        if (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This link has expired");
        }
        if (mapping.getMaxClicks() != null && mapping.getClickCount() >= mapping.getMaxClicks()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Click limit reached for this link");
        }

        // 2. Password Check
        if (mapping.getPassword() != null && !mapping.getPassword().isEmpty()) {
            if (password == null || !urlService.checkPassword(password, mapping.getPassword())) {
                String errorParam = (password != null) ? "&error=1" : "";
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(java.util.Objects.requireNonNull(URI.create("/password.html?c=" + shortCode + errorParam)))
                        .build();
            }
        }

        // 3. Intelligent Routing
        String clientIp = request.getRemoteAddr();
        String country = redirectService.detectCountry(clientIp);
        String device = redirectService.detectDevice(userAgent);
        String browser = redirectService.detectBrowser(userAgent);
        String referrer = request.getHeader("Referer");
        
        String targetUrl = redirectService.determineDestination(mapping, country, device);

        // 4. Final Redirection & Click Increment
        urlService.recordClick(shortCode, clientIp, country, device, browser, referrer);
        
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.util.Objects.requireNonNull(URI.create(targetUrl)))
                .build();
    }
    
    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<DetailedStatsResponse> getStats(@PathVariable String shortCode) {
        UrlMapping mapping = urlService.getStats(shortCode);
        AnalyticsResponse analytics = null;
        // Only provide advanced analytics for registered user links
        if (mapping.getUser() != null) {
            analytics = analyticsService.getAnalytics(mapping);
        }
        return ResponseEntity.ok(new DetailedStatsResponse(mapping, analytics));
    }
    
    @PutMapping("/api/links/{id}")
    public UrlMapping updateLink(@PathVariable Long id, @RequestBody ShortenRequest request, java.security.Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        com.urlshortener.model.User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return urlService.updateUrl(id, request, user);
    }
    
    @PostMapping("/api/links/{id}/toggle")
    public ResponseEntity<Void> toggleLinkStatus(@PathVariable Long id, java.security.Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        com.urlshortener.model.User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        urlService.toggleStatus(id, user);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/api/links/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long id, java.security.Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        com.urlshortener.model.User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        urlService.deleteUrl(id, user);
        return ResponseEntity.ok().build();
    }

    public static class ShortenRequest {
        private String longUrl;
        private long expiresInDays;
        private Long maxClicks;
        private String password;
        private String geoRules;
        private String deviceRules;
        private String customAlias;

        public String getLongUrl() { return longUrl; }
        public void setLongUrl(String longUrl) { this.longUrl = longUrl; }
        public long getExpiresInDays() { return expiresInDays; }
        public void setExpiresInDays(long expiresInDays) { this.expiresInDays = expiresInDays; }
        public Long getMaxClicks() { return maxClicks; }
        public void setMaxClicks(Long maxClicks) { this.maxClicks = maxClicks; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getGeoRules() { return geoRules; }
        public void setGeoRules(String geoRules) { this.geoRules = geoRules; }
        public String getDeviceRules() { return deviceRules; }
        public void setDeviceRules(String deviceRules) { this.deviceRules = deviceRules; }
        public String getCustomAlias() { return customAlias; }
        public void setCustomAlias(String customAlias) { this.customAlias = customAlias; }
    }
}
