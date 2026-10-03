package com.khanh.newsaggregator.article;

import com.khanh.newsaggregator.article.dto.StoryTimelineResponse;
import com.khanh.newsaggregator.article.dto.TimelineEventResponse;
import com.khanh.newsaggregator.common.exception.ResourceNotFoundException;
import com.khanh.newsaggregator.trending.VietnameseKeywordExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryTimelineService {

    private final ArticleRepository articleRepository;
    private final VietnameseKeywordExtractor keywordExtractor;

    public static final String PHASE_GENESIS = "GENESIS";
    public static final String LABEL_GENESIS = "Khởi nguồn";

    public static final String PHASE_PROGRESSION = "PROGRESSION";
    public static final String LABEL_PROGRESSION = "Diễn biến";

    public static final String PHASE_LATEST = "LATEST";
    public static final String LABEL_LATEST = "Mới nhất";

    @Transactional(readOnly = true)
    public StoryTimelineResponse getTimelineForArticle(Long articleId) {
        Article target = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", articleId));

        // 1. Extract entity keywords from title and summary
        String textToExtract = target.getTitle();
        if (target.getSummary() != null && !target.getSummary().isBlank()) {
            textToExtract += " " + target.getSummary();
        }
        Set<String> targetKeywords = keywordExtractor.extractKeywords(textToExtract);

        // 2. Determine 14-day chronological time window around article published date
        LocalDateTime refTime = target.getPublishedAt() != null ? target.getPublishedAt() : target.getFetchedAt();
        if (refTime == null) {
            refTime = LocalDateTime.now();
        }
        LocalDateTime windowStart = refTime.minusDays(14);
        LocalDateTime windowEnd = refTime.plusDays(14);

        // 3. Find candidates within 14 days
        List<Article> candidates = articleRepository.findByPublishedAtBetween(windowStart, windowEnd);

        Long targetCategoryId = target.getCategory() != null ? target.getCategory().getId() : null;

        // 4. Filter and rank related articles (same category or sharing keywords within 14 days)
        List<Article> relatedArticles = candidates.stream()
                .filter(c -> !Objects.equals(c.getId(), target.getId()))
                .filter(c -> isRelated(c, targetCategoryId, targetKeywords))
                .sorted((a1, a2) -> Integer.compare(
                        calculateRelevanceScore(a2, targetCategoryId, targetKeywords),
                        calculateRelevanceScore(a1, targetCategoryId, targetKeywords)
                ))
                .limit(8)
                .collect(Collectors.toList());

        // 5. Assemble all articles including target, deduplicating by ID
        Map<Long, Article> timelineMap = new LinkedHashMap<>();
        for (Article a : relatedArticles) {
            timelineMap.put(a.getId(), a);
        }
        timelineMap.put(target.getId(), target);

        List<Article> sortedTimeline = new ArrayList<>(timelineMap.values());

        // 6. Sort chronologically (publishedAt ASC)
        sortedTimeline.sort(Comparator.comparing(
                a -> a.getPublishedAt() != null ? a.getPublishedAt() : (a.getFetchedAt() != null ? a.getFetchedAt() : LocalDateTime.MIN),
                Comparator.naturalOrder()
        ));

        // 7. Assign phases
        int total = sortedTimeline.size();
        List<TimelineEventResponse> events = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            Article a = sortedTimeline.get(i);
            String phase;
            String phaseLabel;

            if (total == 1) {
                phase = PHASE_LATEST;
                phaseLabel = LABEL_LATEST;
            } else if (i == 0) {
                phase = PHASE_GENESIS;
                phaseLabel = LABEL_GENESIS;
            } else if (i == total - 1) {
                phase = PHASE_LATEST;
                phaseLabel = LABEL_LATEST;
            } else {
                phase = PHASE_PROGRESSION;
                phaseLabel = LABEL_PROGRESSION;
            }

            String sourceName = (a.getSource() != null && a.getSource().getName() != null)
                    ? a.getSource().getName()
                    : "Báo điện tử";

            events.add(TimelineEventResponse.builder()
                    .articleId(a.getId())
                    .title(a.getTitle())
                    .url(a.getUrl())
                    .sourceName(sourceName)
                    .publishedAt(a.getPublishedAt())
                    .phase(phase)
                    .phaseLabel(phaseLabel)
                    .build());
        }

        return StoryTimelineResponse.builder()
                .articleId(target.getId())
                .topicTitle(target.getTitle())
                .events(events)
                .build();
    }

    private boolean isRelated(Article candidate, Long targetCategoryId, Set<String> targetKeywords) {
        // Condition 1: Same category within 14 days
        boolean sameCategory = targetCategoryId != null &&
                candidate.getCategory() != null &&
                targetCategoryId.equals(candidate.getCategory().getId());

        // Condition 2: Sharing entity keywords
        boolean sharesKeywords = false;
        if (targetKeywords != null && !targetKeywords.isEmpty()) {
            String candidateText = candidate.getTitle() + " " + (candidate.getSummary() != null ? candidate.getSummary() : "");
            Set<String> candidateKeywords = keywordExtractor.extractKeywords(candidateText);

            // Exact keyword match
            boolean hasExactMatch = targetKeywords.stream()
                    .anyMatch(kw -> candidateKeywords.stream().anyMatch(ck -> ck.equalsIgnoreCase(kw)));

            // Keyword in candidate title
            String candidateTitleLower = candidate.getTitle().toLowerCase(Locale.ROOT);
            boolean titleContainsKw = targetKeywords.stream()
                    .anyMatch(kw -> candidateTitleLower.contains(kw.toLowerCase(Locale.ROOT)));

            sharesKeywords = hasExactMatch || titleContainsKw;
        }

        return sameCategory || sharesKeywords;
    }

    private int calculateRelevanceScore(Article candidate, Long targetCategoryId, Set<String> targetKeywords) {
        int score = 0;

        // Same category bonus
        if (targetCategoryId != null && candidate.getCategory() != null &&
                targetCategoryId.equals(candidate.getCategory().getId())) {
            score += 15;
        }

        // Shared entity keywords bonus
        if (targetKeywords != null && !targetKeywords.isEmpty()) {
            String candidateText = candidate.getTitle() + " " + (candidate.getSummary() != null ? candidate.getSummary() : "");
            Set<String> candidateKeywords = keywordExtractor.extractKeywords(candidateText);

            long exactMatches = targetKeywords.stream()
                    .filter(kw -> candidateKeywords.stream().anyMatch(ck -> ck.equalsIgnoreCase(kw)))
                    .count();
            score += (int) (exactMatches * 25);

            String candidateTitleLower = candidate.getTitle().toLowerCase(Locale.ROOT);
            long titleMatches = targetKeywords.stream()
                    .filter(kw -> candidateTitleLower.contains(kw.toLowerCase(Locale.ROOT)))
                    .count();
            score += (int) (titleMatches * 20);
        }

        return score;
    }
}
