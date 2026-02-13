package com.urlshortener.repository;

import com.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByShortCode(String shortCode);
    List<UrlMapping> findByUser(com.urlshortener.model.User user);
    List<UrlMapping> findByUserOrderByCreatedAtDesc(com.urlshortener.model.User user);
}
