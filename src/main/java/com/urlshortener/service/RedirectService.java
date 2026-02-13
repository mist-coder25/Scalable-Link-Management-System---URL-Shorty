package com.urlshortener.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.model.UrlMapping;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RedirectService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RedirectService.class);

    private final ObjectMapper objectMapper;

    public RedirectService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Determines the final destination URL based on geo and device rules.
     */
    public String determineDestination(UrlMapping mapping, String countryCode, String deviceType) {
        String targetUrl = mapping.getOriginalUrl();

        // 1. Check Device Rules (Takes precedence in some cases, or we can order them)
        if (mapping.getDeviceRules() != null && !mapping.getDeviceRules().isEmpty()) {
            try {
                Map<String, String> deviceRules = objectMapper.readValue(mapping.getDeviceRules(), new TypeReference<Map<String, String>>() {});
                if (deviceRules.containsKey(deviceType)) {
                    targetUrl = deviceRules.get(deviceType);
                    log.info("Device rule matched: {} -> {}", deviceType, targetUrl);
                }
            } catch (Exception e) {
                log.error("Failed to parse device rules for {}", mapping.getShortCode(), e);
            }
        }

        // 2. Check Geo Rules (Geo rules usually override base destination)
        if (mapping.getGeoRules() != null && !mapping.getGeoRules().isEmpty()) {
            try {
                Map<String, String> geoRules = objectMapper.readValue(mapping.getGeoRules(), new TypeReference<Map<String, String>>() {});
                if (geoRules.containsKey(countryCode)) {
                    targetUrl = geoRules.get(countryCode);
                    log.info("Geo rule matched: {} -> {}", countryCode, targetUrl);
                }
            } catch (Exception e) {
                log.error("Failed to parse geo rules for {}", mapping.getShortCode(), e);
            }
        }

        return targetUrl;
    }

    /**
     * Simple Device Detection Logic
     */
    public String detectDevice(String userAgent) {
        if (userAgent == null) return "desktop";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "mobile";
        }
        return "desktop";
    }

    /**
     * Simple Browser Detection
     */
    public String detectBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("safari/")) return "Safari";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("opr/") || ua.contains("opera/")) return "Opera";
        return "Other";
    }

    /**
     * Simple Geo Detection Logic (Placeholder/Mock)
     * In production, use MaxMind or a GeoIP service.
     */
    public String detectCountry(String ip) {
        // Mock logic for demonstration
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "US"; // Default for local testing
        }
        // In a real app, you'd call an IP -> Geo service here
        return "UNKNOWN";
    }
}
