package com.khanh.newsaggregator.ingestion;

import com.khanh.newsaggregator.source.Source;
import com.khanh.newsaggregator.source.SourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RssIngestionSchedulerTest {

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private RssIngestionService rssIngestionService;

    @Mock
    private com.khanh.newsaggregator.article.ArticleService articleService;

    @InjectMocks
    private RssIngestionScheduler scheduler;

    @Test
    void runIngestionJob_faultIsolation_oneSourceFails_othersShouldContinue() {
        Source brokenSource = Source.builder()
                .id(1L)
                .name("Broken Source")
                .rssUrl("https://invalid-url.com/rss.xml")
                .active(true)
                .build();

        Source healthySource = Source.builder()
                .id(2L)
                .name("Healthy Source")
                .rssUrl("https://vnexpress.net/rss/tin-moi-nhat.rss")
                .active(true)
                .build();

        when(sourceRepository.findByActiveTrue()).thenReturn(List.of(brokenSource, healthySource));
        when(rssIngestionService.ingestSource(brokenSource)).thenThrow(new RuntimeException("Connection timeout"));
        when(rssIngestionService.ingestSource(healthySource)).thenReturn(10);

        // Execute scheduled job
        scheduler.runIngestionJob();

        // Verify that healthy source was still processed despite broken source failing
        verify(rssIngestionService).ingestSource(brokenSource);
        verify(rssIngestionService).ingestSource(healthySource);
        verify(articleService).evictArticlesCache();
    }
}
