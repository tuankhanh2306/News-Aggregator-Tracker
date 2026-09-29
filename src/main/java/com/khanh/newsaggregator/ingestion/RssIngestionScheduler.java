package com.khanh.newsaggregator.ingestion;

import com.khanh.newsaggregator.source.Source;
import com.khanh.newsaggregator.source.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "news.ingestion.enabled", havingValue = "true", matchIfMissing = true)
public class RssIngestionScheduler {

    private final SourceRepository sourceRepository;
    private final RssIngestionService rssIngestionService;
    private final com.khanh.newsaggregator.article.ArticleService articleService;

    /**
     * Scheduled job to fetch RSS feeds across multiple sources.
     * Fault isolation: Each source is processed independently in a try-catch block.
     * One failing source (timeout, 404, invalid format) will never break the entire job.
     */
    @Scheduled(
            fixedDelayString = "${news.ingestion.fixed-delay-ms:300000}",
            initialDelayString = "${news.ingestion.initial-delay-ms:5000}"
    )
    public void runIngestionJob() {
        log.info("========== Starting Scheduled Multi-Source RSS Ingestion Job ==========");

        List<Source> activeSources = sourceRepository.findByActiveTrue();
        if (activeSources.isEmpty()) {
            log.warn("No active RSS source found in database. Skipping job.");
            return;
        }

        int totalNewArticles = 0;
        int successfulSources = 0;
        int failedSources = 0;

        for (Source source : activeSources) {
            try {
                int newCount = rssIngestionService.ingestSource(source);
                totalNewArticles += newCount;
                successfulSources++;
                log.info("✓ Source [id={}, name='{}']: Successfully ingested {} new articles",
                        source.getId(), source.getName(), newCount);
            } catch (Exception e) {
                failedSources++;
                log.error("✗ Source [id={}, name='{}', url='{}'] failed: {}. Continuing with next source...",
                        source.getId(), source.getName(), source.getRssUrl(), e.getMessage());
            }
        }

        if (totalNewArticles > 0) {
            articleService.evictArticlesCache();
        }

        log.info("========== RSS Ingestion Job Finished ==========");
        log.info("Summary: Total active sources: {}, Succeeded: {}, Failed: {}, Total new articles: {}",
                activeSources.size(), successfulSources, failedSources, totalNewArticles);
    }
}
