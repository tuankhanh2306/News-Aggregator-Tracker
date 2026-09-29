package com.khanh.newsaggregator.trending;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "news.trending.enabled", havingValue = "true", matchIfMissing = true)
public class TrendingScheduler {

    private final TrendingService trendingService;

    /**
     * Periodic job to calculate trending keywords across a 6-hour window.
     */
    @Scheduled(
            fixedDelayString = "${news.trending.fixed-delay-ms:1800000}",
            initialDelayString = "${news.trending.initial-delay-ms:8000}"
    )
    public void runTrendingJob() {
        log.info("Running periodic Trending Keywords calculation job (6-hour window)...");
        try {
            trendingService.calculateAndSaveTrending(6, 20);
            log.info("Trending Keywords calculation job finished successfully.");
        } catch (Exception e) {
            log.error("Failed to calculate trending keywords: {}", e.getMessage(), e);
        }
    }
}
