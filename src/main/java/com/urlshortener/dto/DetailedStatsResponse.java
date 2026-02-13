package com.urlshortener.dto;

import com.urlshortener.model.UrlMapping;

public class DetailedStatsResponse {
    private UrlMapping mapping;
    private AnalyticsResponse analytics;

    public DetailedStatsResponse() {}

    public DetailedStatsResponse(UrlMapping mapping, AnalyticsResponse analytics) {
        this.mapping = mapping;
        this.analytics = analytics;
    }

    public UrlMapping getMapping() { return mapping; }
    public void setMapping(UrlMapping mapping) { this.mapping = mapping; }
    public AnalyticsResponse getAnalytics() { return analytics; }
    public void setAnalytics(AnalyticsResponse analytics) { this.analytics = analytics; }
}

