package com.urlshortener.repository;

import com.urlshortener.model.ApiKey;
import com.urlshortener.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByApiKey(String apiKey);
    Optional<ApiKey> findByUser(User user);
}
