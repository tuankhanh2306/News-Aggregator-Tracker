package com.khanh.newsaggregator.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.newsaggregator.article.Article;
import com.khanh.newsaggregator.article.ArticleRepository;
import com.khanh.newsaggregator.article.StoryTimelineService;
import com.khanh.newsaggregator.article.dto.StoryTimelineResponse;
import com.khanh.newsaggregator.article.dto.TimelineEventResponse;
import com.khanh.newsaggregator.category.Category;
import com.khanh.newsaggregator.chat.dto.ChatRequest;
import com.khanh.newsaggregator.chat.dto.ChatResponse;
import com.khanh.newsaggregator.source.Source;
import com.khanh.newsaggregator.trending.TrendingService;
import com.khanh.newsaggregator.trending.VietnameseKeywordExtractor;
import com.khanh.newsaggregator.trending.dto.TrendingKeywordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private StoryTimelineService storyTimelineService;

    @Mock
    private TrendingService trendingService;

    private AiChatService aiChatService;

    private Article sampleArticle1;
    private Article sampleArticle2;

    @BeforeEach
    void setUp() {
        VietnameseKeywordExtractor extractor = new VietnameseKeywordExtractor();
        ObjectMapper objectMapper = new ObjectMapper();
        aiChatService = new AiChatService(
                articleRepository,
                storyTimelineService,
                trendingService,
                extractor,
                objectMapper
        );

        Source source = Source.builder().id(1L).name("VnExpress").build();
        Category catTech = Category.builder().id(2L).name("Công nghệ").slug("cong-nghe").build();

        sampleArticle1 = Article.builder()
                .id(101L)
                .title("Việt Nam đẩy mạnh phát triển chip bán dẫn và AI năm 2026")
                .summary("Nhiều tập đoàn công nghệ lớn đầu tư trung tâm nghiên cứu AI và bán dẫn tại Hà Nội và TP HCM.")
                .url("https://vnexpress.net/chip-ai-101.html")
                .source(source)
                .category(catTech)
                .publishedAt(LocalDateTime.now().minusHours(2))
                .build();

        sampleArticle2 = Article.builder()
                .id(102L)
                .title("Giá vàng SJC biến động mạnh trong phiên giao dịch sáng nay")
                .summary("Thị trường vàng trong nước ghi nhận nhịp điều chỉnh theo giá vàng thế giới.")
                .url("https://vnexpress.net/gia-vang-102.html")
                .source(source)
                .category(Category.builder().id(3L).name("Kinh doanh").slug("kinh-doanh").build())
                .publishedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    void testAskCurrentArticleSummary() {
        when(articleRepository.findById(101L)).thenReturn(Optional.of(sampleArticle1));
        when(articleRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleArticle1, sampleArticle2)));

        StoryTimelineResponse timeline = StoryTimelineResponse.builder()
                .articleId(101L)
                .events(List.of(
                        TimelineEventResponse.builder()
                                .articleId(100L)
                                .title("Khởi động chiến lược bán dẫn quốc gia")
                                .phase("GENESIS")
                                .phaseLabel("Khởi nguồn")
                                .sourceName("VnExpress")
                                .publishedAt(LocalDateTime.now().minusDays(2))
                                .build(),
                        TimelineEventResponse.builder()
                                .articleId(101L)
                                .title(sampleArticle1.getTitle())
                                .phase("LATEST")
                                .phaseLabel("Mới nhất")
                                .sourceName("VnExpress")
                                .publishedAt(sampleArticle1.getPublishedAt())
                                .build()
                ))
                .build();
        when(storyTimelineService.getTimelineForArticle(101L)).thenReturn(timeline);

        ChatRequest req = ChatRequest.builder()
                .message("Tóm tắt tin đang xem")
                .currentArticleId(101L)
                .build();

        ChatResponse res = aiChatService.askAssistant(req);

        assertNotNull(res);
        assertEquals("LOCAL_EDITORIAL", res.getEngine());
        assertTrue(res.getReply().contains("Điểm cốt lõi bài đang đọc"));
        assertTrue(res.getReply().contains("Công nghệ"));
        assertFalse(res.getRelatedArticles().isEmpty());
        assertEquals(101L, res.getRelatedArticles().get(0).getId());
    }

    @Test
    void testAskTimelineArc() {
        when(articleRepository.findById(101L)).thenReturn(Optional.of(sampleArticle1));
        when(articleRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleArticle1, sampleArticle2)));

        StoryTimelineResponse timeline = StoryTimelineResponse.builder()
                .articleId(101L)
                .events(List.of(
                        TimelineEventResponse.builder()
                                .articleId(100L)
                                .title("Khởi động chiến lược bán dẫn")
                                .phase("GENESIS")
                                .phaseLabel("Khởi nguồn")
                                .sourceName("VnExpress")
                                .publishedAt(LocalDateTime.now().minusDays(3))
                                .build(),
                        TimelineEventResponse.builder()
                                .articleId(101L)
                                .title(sampleArticle1.getTitle())
                                .phase("LATEST")
                                .phaseLabel("Mới nhất")
                                .sourceName("VnExpress")
                                .publishedAt(LocalDateTime.now().minusHours(1))
                                .build()
                ))
                .build();
        when(storyTimelineService.getTimelineForArticle(101L)).thenReturn(timeline);

        ChatRequest req = ChatRequest.builder()
                .message("Diễn biến sự kiện này")
                .currentArticleId(101L)
                .build();

        ChatResponse res = aiChatService.askAssistant(req);

        assertNotNull(res);
        assertTrue(res.getReply().contains("Toàn cảnh dòng sự kiện"));
        assertTrue(res.getReply().contains("Khởi nguồn"));
        assertTrue(res.getReply().contains("Mới nhất"));
    }

    @Test
    void testAskTopicSearchAndTrending() {
        when(articleRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleArticle1, sampleArticle2)));
        when(trendingService.getTrendingKeywords(8))
                .thenReturn(List.of(new TrendingKeywordResponse("Việt Nam", 12), new TrendingKeywordResponse("SJC", 8)));

        ChatRequest searchReq = ChatRequest.builder()
                .message("Tình hình giá vàng SJC")
                .build();

        ChatResponse searchRes = aiChatService.askAssistant(searchReq);
        assertNotNull(searchRes);
        assertTrue(searchRes.getReply().contains("Giá vàng SJC"));
        assertFalse(searchRes.getRelatedArticles().isEmpty());

        ChatRequest trendReq = ChatRequest.builder()
                .message("Chủ đề xu hướng hôm nay")
                .build();

        ChatResponse trendRes = aiChatService.askAssistant(trendReq);
        assertNotNull(trendRes);
        assertTrue(trendRes.getReply().contains("#Việt Nam"));
    }
}
