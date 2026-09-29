package com.khanh.newsaggregator.digest;

import com.khanh.newsaggregator.article.Article;
import com.khanh.newsaggregator.article.ArticleRepository;
import com.khanh.newsaggregator.common.exception.ResourceNotFoundException;
import com.khanh.newsaggregator.digest.dto.ArticleDigestDto;
import com.khanh.newsaggregator.digest.dto.CategoryDigestDto;
import com.khanh.newsaggregator.subscription.Subscription;
import com.khanh.newsaggregator.subscription.SubscriptionRepository;
import com.khanh.newsaggregator.user.AppUser;
import com.khanh.newsaggregator.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDigestService {

    private final SubscriptionRepository subscriptionRepository;
    private final AppUserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final EmailService emailService;

    private static final String DIGEST_TEMPLATE = "mail/daily-digest";

    @Transactional(readOnly = true)
    public Map<String, Object> buildDigestModel(String userEmail) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        List<Subscription> subscriptions = subscriptionRepository.findWithCategoryByUserId(user.getId());
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        List<CategoryDigestDto> categoryDigests = new ArrayList<>();

        for (Subscription sub : subscriptions) {
            Long categoryId = sub.getCategory().getId();
            List<Article> articles = articleRepository.findTop10ByCategoryIdAndPublishedAtAfterOrderByPublishedAtDesc(categoryId, since);

            if (articles.isEmpty()) {
                articles = articleRepository.findTop10ByCategoryIdOrderByPublishedAtDesc(categoryId);
            }

            if (!articles.isEmpty()) {
                List<ArticleDigestDto> articleDtos = articles.stream()
                        .limit(5)
                        .map(a -> ArticleDigestDto.builder()
                                .title(a.getTitle())
                                .summary(a.getSummary())
                                .url(a.getUrl())
                                .sourceName(a.getSource() != null ? a.getSource().getName() : "Tin tức")
                                .publishedAt(a.getPublishedAt())
                                .build())
                        .toList();

                categoryDigests.add(CategoryDigestDto.builder()
                        .categoryName(sub.getCategory().getName())
                        .articles(articleDtos)
                        .build());
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("recipientEmail", user.getEmail());
        model.put("digestDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        model.put("categoryDigests", categoryDigests);

        return model;
    }

    public void sendDailyDigestToUser(String userEmail) {
        Map<String, Object> model = buildDigestModel(userEmail);
        String subject = "📰 News Aggregator — Bản tin cập nhật hằng ngày";
        emailService.sendHtmlEmail(userEmail, subject, DIGEST_TEMPLATE, model);
    }

    public String previewDigestForUser(String userEmail) {
        Map<String, Object> model = buildDigestModel(userEmail);
        return emailService.renderEmailPreview(DIGEST_TEMPLATE, model);
    }

    @Transactional(readOnly = true)
    public int sendDailyDigestToAllSubscribers() {
        log.info("Starting Daily Digest delivery job to all subscribed users...");

        List<Subscription> allSubscriptions = subscriptionRepository.findAllWithUserAndCategory();
        Set<String> recipientEmails = new HashSet<>();
        for (Subscription sub : allSubscriptions) {
            recipientEmails.add(sub.getUser().getEmail());
        }

        log.info("Found {} distinct subscribed users for email digest.", recipientEmails.size());

        int successCount = 0;
        int failureCount = 0;

        for (String email : recipientEmails) {
            try {
                sendDailyDigestToUser(email);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send daily digest to user {}: {}", email, e.getMessage());
                failureCount++;
            }
        }

        log.info("Daily Digest delivery job finished: {} succeeded, {} failed.", successCount, failureCount);
        return successCount;
    }
}
