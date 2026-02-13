package com.urlshortener.repository;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByUrlMappingOrderByTimestampDesc(UrlMapping urlMapping);
}
