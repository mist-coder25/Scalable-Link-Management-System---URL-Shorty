package com.urlshortener.service;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UrlService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UrlService.class);

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisTemplate;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final ClickEventRepository clickEventRepository;

    public UrlService(UrlRepository urlRepository, StringRedisTemplate redisTemplate,
                      org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                      ClickEventRepository clickEventRepository) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.clickEventRepository = clickEventRepository;
    }

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final long CACHE_TTL_DAYS = 7;

    @Transactional
    public UrlMapping shortenUrl(com.urlshortener.controller.UrlController.ShortenRequest request, com.urlshortener.model.User user) {
        // 1. Check Custom Alias Availability
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Custom aliases are only available for registered users");
            }
            String alias = request.getCustomAlias().trim();
            if (!alias.matches("^[a-zA-Z0-9_-]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid alias format. Only letters, numbers, hyphens, and underscores allowed.");
            }
            if (urlRepository.findByShortCode(alias).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Alias '" + alias + "' is already taken");
            }
            // Use custom alias
            return createUrlMapping(request, user, alias);
        }

        // 2. Generate Standard Short Code
        // Temporary placeholder to satisfy NotNull constraint before Base62 generation
        // We will create the mapping with a temp code, save it to get ID, then update with Base62
        String tempCode = java.util.UUID.randomUUID().toString().substring(0, 8);
        UrlMapping savedMapping = createUrlMapping(request, user, tempCode);
        
        String shortCode = encodeBase62(savedMapping.getId());
        savedMapping.setShortCode(shortCode);
        savedMapping = urlRepository.save(savedMapping);
        
        cacheUrl(shortCode, savedMapping.getOriginalUrl());
        
        return savedMapping;
    }
    
    private UrlMapping createUrlMapping(com.urlshortener.controller.UrlController.ShortenRequest request, com.urlshortener.model.User user, String shortCode) {
        long days = request.getExpiresInDays() > 0 ? request.getExpiresInDays() : 30;
        
        // Enforce guest constraints
        if (user == null) {
            days = 7; // Guest links always expire in 7 days
        }
        
        String password = null;
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            password = passwordEncoder.encode(request.getPassword());
        }

        if (user != null) {
            log.info("Creating link for user: {}", user.getEmail());
        } else {
            log.info("Creating link for guest");
        }

        UrlMapping mapping = new UrlMapping(
                request.getLongUrl(),
                shortCode,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(days),
                user,
                0L,
                true,
                request.getMaxClicks(),
                password,
                request.getGeoRules(),
                request.getDeviceRules()
        );
        UrlMapping saved = urlRepository.save(mapping);
        log.info("Link saved with ID: {}", saved.getId());
        
        // Cache immediately if it's the final code (custom alias)
        // For standard flow, we cache after update in the main method
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
             cacheUrl(shortCode, request.getLongUrl());
        }
        
        return saved;
    }

    public List<UrlMapping> getLinksByUser(com.urlshortener.model.User user) {
        return urlRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public String getOriginalUrl(String shortCode) {
        // 1. Check Redis Cache
        String cachedUrl = redisTemplate.opsForValue().get("url:" + shortCode);
        if (cachedUrl != null) {
            recordClick(shortCode, "0.0.0.0", "Unknown", "Unknown", "Unknown", "Referrer");
            return cachedUrl;
        }

        // 2. Check Database
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));

        if (mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "URL has expired");
        }

        // 3. Cache and Return
        cacheUrl(shortCode, mapping.getOriginalUrl());
        recordClick(shortCode, "0.0.0.0", "Unknown", "Unknown", "Unknown", "Referrer");

        return mapping.getOriginalUrl();
    }
    
    public UrlMapping getStats(String shortCode) {
         return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));
    }

    private void cacheUrl(String shortCode, String originalUrl) {
        redisTemplate.opsForValue().set("url:" + shortCode, java.util.Objects.requireNonNull(originalUrl), java.util.Objects.requireNonNull(Duration.ofDays(CACHE_TTL_DAYS)));
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Async
    public void recordClick(String shortCode, String ip, String country, String device, String browser, String referrer) {
        try {
            Optional<UrlMapping> opt = urlRepository.findByShortCode(shortCode);
            if (opt.isPresent()) {
                UrlMapping mapping = opt.get();
                mapping.setClickCount(mapping.getClickCount() + 1);
                urlRepository.save(mapping);

                ClickEvent event = new ClickEvent(
                        mapping,
                        LocalDateTime.now(),
                        ip,
                        country,
                        device,
                        browser,
                        referrer
                );
                clickEventRepository.save(event);
            }
        } catch (Exception e) {
            log.error("Failed to record click for {}", shortCode, e);
        }
    }

    @Transactional
    public UrlMapping updateUrl(Long id, com.urlshortener.controller.UrlController.ShortenRequest request, com.urlshortener.model.User user) {
        UrlMapping mapping = urlRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));

        if (mapping.getUser() == null || !mapping.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        mapping.setOriginalUrl(request.getLongUrl());
        mapping.setMaxClicks(request.getMaxClicks());
        mapping.setGeoRules(request.getGeoRules());
        mapping.setDeviceRules(request.getDeviceRules());
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            mapping.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        UrlMapping updated = urlRepository.save(mapping);
        cacheUrl(updated.getShortCode(), updated.getOriginalUrl()); // Refresh cache
        return updated;
    }

    @Transactional
    public void toggleStatus(Long id, com.urlshortener.model.User user) {
        UrlMapping mapping = urlRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));

        if (mapping.getUser() == null || !mapping.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        mapping.setActive(!mapping.isActive());
        urlRepository.save(mapping);
    }

    @Transactional
    public void deleteUrl(Long id, com.urlshortener.model.User user) {
        UrlMapping mapping = urlRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));

        if (mapping.getUser() == null || !mapping.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        redisTemplate.delete("url:" + mapping.getShortCode());
        urlRepository.delete(mapping);
    }

    private String encodeBase62(long id) {
        StringBuilder sb = new StringBuilder();
        if (id == 0) return String.valueOf(BASE62.charAt(0));
        while (id > 0) {
            sb.append(BASE62.charAt((int) (id % 62)));
            id /= 62;
        }
        return sb.reverse().toString();
    }
}
