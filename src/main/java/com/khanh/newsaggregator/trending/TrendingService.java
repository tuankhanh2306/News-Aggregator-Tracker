package com.khanh.newsaggregator.trending;

import com.khanh.newsaggregator.article.Article;
import com.khanh.newsaggregator.article.ArticleRepository;
import com.khanh.newsaggregator.config.RedisConfig;
import com.khanh.newsaggregator.trending.dto.TrendingKeywordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingService {

    private final ArticleRepository articleRepository;
    private final KeywordStatRepository keywordStatRepository;
    private final VietnameseKeywordExtractor keywordExtractor;

    /**
     * Get top trending keywords from Redis cache or database.
     */
    @Cacheable(value = RedisConfig.CACHE_TRENDING, key = "#limit")
    @Transactional
    public List<TrendingKeywordResponse> getTrendingKeywords(int limit) {
        log.info("Fetching trending keywords (Cache Miss) with limit={}", limit);

        // Check if stats exist in the last 24 hours
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<KeywordStat> stats = keywordStatRepository.findTopKeywordsSince(since);

        if (stats.isEmpty()) {
            // Recalculate if no stats exist yet
            return calculateTrendingKeywords(24, limit);
        }

        return stats.stream()
                .limit(limit)
                .map(k -> TrendingKeywordResponse.builder()
                        .keyword(k.getKeyword())
                        .count(k.getCount())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calculate trending keywords from article titles, save to database and refresh Redis cache.
     *
     * @param hoursWindow time window in hours (e.g. 6 or 24 hours)
     * @param limit number of top keywords to keep
     * @return top trending keywords
     */
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_TRENDING, allEntries = true)
    public List<TrendingKeywordResponse> calculateAndSaveTrending(int hoursWindow, int limit) {
        log.info("Calculating trending keywords for the last {} hours...", hoursWindow);
        return calculateTrendingKeywords(hoursWindow, limit);
    }

    private List<TrendingKeywordResponse> calculateTrendingKeywords(int hoursWindow, int limit) {
        LocalDateTime windowStart = LocalDateTime.now().minusHours(hoursWindow);
        List<Article> articles = articleRepository.findByPublishedAtAfter(windowStart);

        if (articles.isEmpty()) {
            log.info("No articles published in the last {} hours. Falling back to latest 500 articles.", hoursWindow);
            articles = articleRepository.findTop500ByOrderByPublishedAtDesc();
        }

        if (articles.isEmpty()) {
            log.warn("No articles found in database to calculate trending keywords.");
            return Collections.emptyList();
        }

        Map<String, Integer> freqMap = new HashMap<>();

        for (Article article : articles) {
            Set<String> keywords = keywordExtractor.extractKeywords(article.getTitle());
            for (String kw : keywords) {
                freqMap.merge(kw, 1, Integer::sum);
            }
        }

        List<TrendingKeywordResponse> topKeywords = freqMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> TrendingKeywordResponse.builder()
                        .keyword(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // Save to keyword_stat table
        List<KeywordStat> statEntities = topKeywords.stream()
                .map(item -> KeywordStat.builder()
                        .keyword(item.getKeyword())
                        .count(item.getCount())
                        .windowStart(windowStart)
                        .build())
                .collect(Collectors.toList());

        keywordStatRepository.saveAll(statEntities);
        log.info("Saved {} trending keywords to keyword_stat for window starting at {}", statEntities.size(), windowStart);

        return topKeywords;
    }
}
