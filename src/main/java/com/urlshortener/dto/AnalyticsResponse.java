package com.urlshortener.dto;

import java.util.Map;

public class AnalyticsResponse {
    private long totalClicks;
    private Map<String, Long> clicksByDate;     // "YYYY-MM-DD" -> count
    private Map<String, Long> clicksByCountry;
    private Map<String, Long> clicksByDevice;
    private Map<String, Long> clicksByBrowser;
    private Map<String, Long> topReferrers;

    public AnalyticsResponse() {}

    public AnalyticsResponse(long totalClicks, Map<String, Long> clicksByDate, Map<String, Long> clicksByCountry,
                             Map<String, Long> clicksByDevice, Map<String, Long> clicksByBrowser, Map<String, Long> topReferrers) {
        this.totalClicks = totalClicks;
        this.clicksByDate = clicksByDate;
        this.clicksByCountry = clicksByCountry;
        this.clicksByDevice = clicksByDevice;
        this.clicksByBrowser = clicksByBrowser;
        this.topReferrers = topReferrers;
    }

    public long getTotalClicks() { return totalClicks; }
    public void setTotalClicks(long totalClicks) { this.totalClicks = totalClicks; }
    public Map<String, Long> getClicksByDate() { return clicksByDate; }
    public void setClicksByDate(Map<String, Long> clicksByDate) { this.clicksByDate = clicksByDate; }
    public Map<String, Long> getClicksByCountry() { return clicksByCountry; }
    public void setClicksByCountry(Map<String, Long> clicksByCountry) { this.clicksByCountry = clicksByCountry; }
    public Map<String, Long> getClicksByDevice() { return clicksByDevice; }
    public void setClicksByDevice(Map<String, Long> clicksByDevice) { this.clicksByDevice = clicksByDevice; }
    public Map<String, Long> getClicksByBrowser() { return clicksByBrowser; }
    public void setClicksByBrowser(Map<String, Long> clicksByBrowser) { this.clicksByBrowser = clicksByBrowser; }
    public Map<String, Long> getTopReferrers() { return topReferrers; }
    public void setTopReferrers(Map<String, Long> topReferrers) { this.topReferrers = topReferrers; }
}
