package com.urlshortener.controller;

import com.urlshortener.model.ApiKey;
import com.urlshortener.model.User;
import com.urlshortener.repository.ApiKeyRepository;
import com.urlshortener.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/developer/keys")
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyController(ApiKeyRepository apiKeyRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<ApiKeyDto> getMyKeys(Principal principal) {
        User user = getUser(principal);
        return apiKeyRepository.findByUser(user).stream()
                .map(key -> new ApiKeyDto(key.getId(), key.getApiKey(), key.getName(), key.isActive(), key.getCreatedAt().toString()))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ApiKeyCreateResponse generateKey(@RequestBody ApiKeyCreateRequest request, Principal principal) {
        User user = getUser(principal);
        
        String rawKey = UUID.randomUUID().toString().replace("-", "");
        String rawSecret = UUID.randomUUID().toString().replace("-", "");
        
        ApiKey apiKey = new ApiKey(
                rawKey,
                passwordEncoder.encode(rawSecret),
                request.getName(),
                user
        );
        
        apiKeyRepository.save(apiKey);
        
        return new ApiKeyCreateResponse(rawKey, rawSecret);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable Long id, Principal principal) {
        User user = getUser(principal);
        ApiKey key = apiKeyRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (key.getUser() == null || !key.getUser().getId().equals(user.getId())) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        
        apiKeyRepository.delete(key);
        return ResponseEntity.ok().build();
    }

    private User getUser(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public static class ApiKeyDto {
        private Long id;
        private String apiKey;
        private String name;
        private boolean active;
        private String createdAt;

        public ApiKeyDto(Long id, String apiKey, String name, boolean active, String createdAt) {
            this.id = id;
            this.apiKey = apiKey;
            this.name = name;
            this.active = active;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getApiKey() { return apiKey; }
        public String getName() { return name; }
        public boolean isActive() { return active; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class ApiKeyCreateRequest {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class ApiKeyCreateResponse {
        private String apiKey;
        private String apiSecret;

        public ApiKeyCreateResponse(String apiKey, String apiSecret) {
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
        }

        public String getApiKey() { return apiKey; }
        public String getApiSecret() { return apiSecret; }
    }
}
