package com.khanh.newsaggregator.digest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "news.digest.enabled", havingValue = "true", matchIfMissing = true)
public class DigestScheduler {

    private final EmailDigestService emailDigestService;

    @Scheduled(cron = "${news.digest.cron:0 0 7 * * *}")
    public void runDailyDigestJob() {
        log.info("Executing scheduled Daily Email Digest job...");
        try {
            int delivered = emailDigestService.sendDailyDigestToAllSubscribers();
            log.info("Scheduled Daily Email Digest job completed. Delivered to {} users.", delivered);
        } catch (Exception e) {
            log.error("Scheduled Daily Email Digest job encountered an error: {}", e.getMessage(), e);
        }
    }
}
