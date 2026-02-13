package com.urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final com.urlshortener.service.CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                          com.urlshortener.service.CustomOAuth2UserService customOAuth2UserService) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simplicity in this demo (stateless REST-like)
            .addFilterBefore(apiKeyAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/style.css", "/app.js", "/style_append.css").permitAll() // Static files
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll() // Auth endpoints
                .requestMatchers(HttpMethod.GET, "/{shortCode:[a-zA-Z0-9_-]+}").permitAll() // Short links
                .requestMatchers(HttpMethod.GET, "/api/stats/**").permitAll() // Public stats
                .anyRequest().permitAll() // Allow shortening without login, or change to authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/dashboard.html", true)
            );
            
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
