package com.urlshortener.config;

import com.urlshortener.model.ApiKey;
import com.urlshortener.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, @org.springframework.context.annotation.Lazy PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String apiKeyHeader = request.getHeader("X-API-KEY");
        String apiSecretHeader = request.getHeader("X-API-SECRET");

        if (apiKeyHeader != null && apiSecretHeader != null) {
            Optional<ApiKey> optKey = apiKeyRepository.findByApiKey(apiKeyHeader);
            
            if (optKey.isPresent()) {
                ApiKey apiKey = optKey.get();
                if (apiKey.isActive() && passwordEncoder.matches(apiSecretHeader, apiKey.getApiSecret())) {
                    try {
                        String email = apiKey.getUser().getEmail();
                        // System.out.println("DEBUG: Auth Filter User Email: " + email);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                email, null, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception e) {
                        System.err.println("DEBUG: CRASH in Auth Filter accessing User: " + e.getMessage());
                        throw e;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
