package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.ClickEventRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public AnalyticsService(ClickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    public AnalyticsResponse getAnalytics(UrlMapping mapping) {
        List<ClickEvent> events = clickEventRepository.findByUrlMappingOrderByTimestampDesc(mapping);

        Map<String, Long> clicksByDate = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getTimestamp().format(DATE_FORMATTER),
                        Collectors.counting()
                ));

        Map<String, Long> clicksByCountry = events.stream()
                .filter(e -> e.getCountry() != null)
                .collect(Collectors.groupingBy(ClickEvent::getCountry, Collectors.counting()));

        Map<String, Long> clicksByDevice = events.stream()
                .filter(e -> e.getDevice() != null)
                .collect(Collectors.groupingBy(ClickEvent::getDevice, Collectors.counting()));

        Map<String, Long> clicksByBrowser = events.stream()
                .filter(e -> e.getBrowser() != null)
                .collect(Collectors.groupingBy(ClickEvent::getBrowser, Collectors.counting()));

        Map<String, Long> topReferrers = events.stream()
                .filter(e -> e.getReferrer() != null && !e.getReferrer().isEmpty())
                .collect(Collectors.groupingBy(ClickEvent::getReferrer, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new AnalyticsResponse(
                mapping.getClickCount(),
                clicksByDate,
                clicksByCountry,
                clicksByDevice,
                clicksByBrowser,
                topReferrers
        );
    }

    public AnalyticsResponse getGlobalAnalytics(List<UrlMapping> mappings) {
        List<ClickEvent> allEvents = mappings.stream()
                .flatMap(m -> clickEventRepository.findByUrlMappingOrderByTimestampDesc(m).stream())
                .collect(Collectors.toList());

        long totalClicks = mappings.stream().mapToLong(UrlMapping::getClickCount).sum();

        Map<String, Long> clicksByDate = allEvents.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getTimestamp().format(DATE_FORMATTER),
                        Collectors.counting()
                ));

        Map<String, Long> clicksByCountry = allEvents.stream()
                .filter(e -> e.getCountry() != null)
                .collect(Collectors.groupingBy(ClickEvent::getCountry, Collectors.counting()));

        Map<String, Long> clicksByDevice = allEvents.stream()
                .filter(e -> e.getDevice() != null)
                .collect(Collectors.groupingBy(ClickEvent::getDevice, Collectors.counting()));

        Map<String, Long> clicksByBrowser = allEvents.stream()
                .filter(e -> e.getBrowser() != null)
                .collect(Collectors.groupingBy(ClickEvent::getBrowser, Collectors.counting()));

        Map<String, Long> topReferrers = allEvents.stream()
                .filter(e -> e.getReferrer() != null && !e.getReferrer().isEmpty())
                .collect(Collectors.groupingBy(ClickEvent::getReferrer, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new AnalyticsResponse(
                totalClicks,
                clicksByDate,
                clicksByCountry,
                clicksByDevice,
                clicksByBrowser,
                topReferrers
        );
    }
}
