package com.urlshortener.controller;

import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.urlshortener.model.ApiKey;
import com.urlshortener.repository.ApiKeyRepository;
import java.util.UUID;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyRepository apiKeyRepository;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder passwordEncoder, ApiKeyRepository apiKeyRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.apiKeyRepository = apiKeyRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        try {
            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }

            // Create new user's account
            User user = new User(
                    signUpRequest.getEmail(),
                    passwordEncoder.encode(signUpRequest.getPassword()),
                    signUpRequest.getFullName(),
                    LocalDateTime.now()
            );

            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Signup Error: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // Generate or update API Key for session
            ApiKey apiKey = apiKeyRepository.findByUser(user).orElse(new ApiKey());
            if (apiKey.getUser() == null) {
                apiKey.setUser(user);
                apiKey.setCreatedAt(LocalDateTime.now());
            }
            
            // Generate new credentials
            String rawKey = UUID.randomUUID().toString();
            String rawSecret = UUID.randomUUID().toString();
            
            apiKey.setApiKey(rawKey);
            apiKey.setApiSecret(passwordEncoder.encode(rawSecret));
            apiKey.setName("Web Login " + LocalDateTime.now());
            apiKey.setActive(true);
            
            apiKeyRepository.save(apiKey);
            
            return ResponseEntity.ok(new AuthResponse(
                "Login successful", 
                rawKey, 
                rawSecret, 
                user.getEmail(), 
                user.getFullName()
            ));
        } catch (org.springframework.security.core.AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid email or password"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Login Error: " + e.getMessage()));
        }
    }

    static class SignupRequest {
        private String fullName;
        private String email;
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class MessageResponse {
        private String message;
        public MessageResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    @org.springframework.web.bind.annotation.GetMapping("/keys")
    public ResponseEntity<?> getSessionKeys(java.security.Principal principal) {
        if (principal == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Not logged in"));
        }
        
        String email;
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            email = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal).getPrincipal().getAttribute("email");
        } else {
            email = principal.getName();
        }
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                
        // Find existing key or create new one
        ApiKey apiKey = apiKeyRepository.findByUser(user).orElseGet(() -> {
            ApiKey newKey = new ApiKey();
            newKey.setUser(user);
            newKey.setCreatedAt(LocalDateTime.now());
            newKey.setApiKey(UUID.randomUUID().toString());
            // Store raw secret temporarily or manage differently. 
            // For OAuth users, we can't show the secret again if hashed.
            // So we might need to regenerate if it's a fresh session and they don't have it locally.
            // For simplicity, we regenerate if missing, or return existing (but we can't return hashed secret).
            // Actually, we must regenerate if the user doesn't have it.
            // Let's ALWAYS regenerate for this endpoint for security or check if we can return a stored one? No, stored is hashed.
            return newKey;
        });
        
        // If it's a new key object (from orElseGet) or we want to ensure they have a working key:
        // We will regenerate the secret and key to ensure the user gets a valid pair they can use.
        String rawKey = UUID.randomUUID().toString();
        String rawSecret = UUID.randomUUID().toString();
        
        apiKey.setApiKey(rawKey);
        apiKey.setApiSecret(passwordEncoder.encode(rawSecret));
        apiKey.setName("OAuth/Session Key " + LocalDateTime.now());
        apiKey.setActive(true);
        apiKeyRepository.save(apiKey);
        
        return ResponseEntity.ok(new AuthResponse(
            "Keys generated", 
            rawKey, 
            rawSecret, 
            user.getEmail(), 
            user.getFullName()
        ));
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<?> getUserId(@org.springframework.web.bind.annotation.RequestHeader(value = "X-API-KEY", required = false) String apiKey,
                                       @org.springframework.web.bind.annotation.RequestHeader(value = "X-API-SECRET", required = false) String apiSecret) {
        if (apiKey == null || apiSecret == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Missing Auth Headers"));
        }
        
        return apiKeyRepository.findByApiKey(apiKey)
            .map(key -> {
                if (passwordEncoder.matches(apiSecret, key.getApiSecret())) {
                     User user = key.getUser();
                     return ResponseEntity.ok(new AuthResponse("Validated", apiKey, "HIDDEN", user.getEmail(), user.getFullName()));
                } else {
                     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Invalid Secret"));
                }
            })
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Invalid Key")));
    }

    static class AuthResponse {
        private String message;
        private String apiKey;
        private String apiSecret;
        private String email;
        private String fullName;

        public AuthResponse(String message, String apiKey, String apiSecret, String email, String fullName) {
            this.message = message;
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
            this.email = email;
            this.fullName = fullName;
        }

        public String getMessage() { return message; }
        public String getApiKey() { return apiKey; }
        public String getApiSecret() { return apiSecret; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
    }
}
