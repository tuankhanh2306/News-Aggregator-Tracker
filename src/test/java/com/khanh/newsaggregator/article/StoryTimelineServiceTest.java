package com.khanh.newsaggregator.article;

import com.khanh.newsaggregator.article.dto.StoryTimelineResponse;
import com.khanh.newsaggregator.article.dto.TimelineEventResponse;
import com.khanh.newsaggregator.category.Category;
import com.khanh.newsaggregator.common.exception.ResourceNotFoundException;
import com.khanh.newsaggregator.source.Source;
import com.khanh.newsaggregator.trending.VietnameseKeywordExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryTimelineServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Spy
    private VietnameseKeywordExtractor keywordExtractor = new VietnameseKeywordExtractor();

    @InjectMocks
    private StoryTimelineService storyTimelineService;

    private Category kinhDoanhCategory;
    private Category thoiSuCategory;
    private Source vnExpress;
    private Source tuoiTre;

    @BeforeEach
    void setUp() {
        kinhDoanhCategory = Category.builder().id(1L).name("Kinh doanh").slug("kinh-doanh").build();
        thoiSuCategory = Category.builder().id(2L).name("Thời sự").slug("thoi-su").build();
        vnExpress = Source.builder().id(1L).name("VnExpress").build();
        tuoiTre = Source.builder().id(2L).name("Tuổi Trẻ").build();
    }

    @Test
    void getTimelineForArticle_whenArticlesFound_shouldReturnChronologicalEventsWithPhases() {
        // Target article: Oct 3
        Article targetArticle = Article.builder()
                .id(103L)
                .title("Giá vàng hạ nhiệt về vùng 87 triệu đồng")
                .summary("Thị trường vàng SJC trong nước đảo chiều sau chỉ đạo của Ngân hàng Nhà nước.")
                .url("https://vnexpress.net/gia-vang-103")
                .source(vnExpress)
                .category(kinhDoanhCategory)
                .publishedAt(LocalDateTime.of(2026, 10, 3, 9, 0))
                .build();

        // Earliest event: Oct 1
        Article genesisArticle = Article.builder()
                .id(101L)
                .title("Giá vàng miếng SJC chạm đỉnh lịch sử 91 triệu")
                .summary("Nhu cầu mua vàng SJC tăng đột biến trên toàn quốc.")
                .url("https://tuoitre.vn/gia-vang-101")
                .source(tuoiTre)
                .category(kinhDoanhCategory)
                .publishedAt(LocalDateTime.of(2026, 10, 1, 8, 30))
                .build();

        // Progression event: Oct 2
        Article progressionArticle = Article.builder()
                .id(102L)
                .title("Ngân hàng Nhà nước can thiệp bình ổn vàng SJC")
                .summary("Nhà điều hành chuẩn bị đấu thầu vàng miếng SJC để hạ nhiệt thị trường.")
                .url("https://vnexpress.net/ngan-hang-102")
                .source(vnExpress)
                .category(thoiSuCategory) // Cross-category with shared entity "SJC"
                .publishedAt(LocalDateTime.of(2026, 10, 2, 14, 0))
                .build();

        when(articleRepository.findById(103L)).thenReturn(Optional.of(targetArticle));
        when(articleRepository.findByPublishedAtBetween(any(), any()))
                .thenReturn(List.of(genesisArticle, progressionArticle, targetArticle));

        StoryTimelineResponse response = storyTimelineService.getTimelineForArticle(103L);

        assertThat(response).isNotNull();
        assertThat(response.getArticleId()).isEqualTo(103L);
        assertThat(response.getEvents()).hasSize(3);

        List<TimelineEventResponse> events = response.getEvents();

        // Check chronological order (ASC)
        assertThat(events.get(0).getArticleId()).isEqualTo(101L);
        assertThat(events.get(0).getPhase()).isEqualTo(StoryTimelineService.PHASE_GENESIS);
        assertThat(events.get(0).getPhaseLabel()).isEqualTo(StoryTimelineService.LABEL_GENESIS);

        assertThat(events.get(1).getArticleId()).isEqualTo(102L);
        assertThat(events.get(1).getPhase()).isEqualTo(StoryTimelineService.PHASE_PROGRESSION);
        assertThat(events.get(1).getPhaseLabel()).isEqualTo(StoryTimelineService.LABEL_PROGRESSION);

        assertThat(events.get(2).getArticleId()).isEqualTo(103L);
        assertThat(events.get(2).getPhase()).isEqualTo(StoryTimelineService.PHASE_LATEST);
        assertThat(events.get(2).getPhaseLabel()).isEqualTo(StoryTimelineService.LABEL_LATEST);
    }

    @Test
    void getTimelineForArticle_whenSingleArticle_shouldAssignLatestPhase() {
        Article singleArticle = Article.builder()
                .id(200L)
                .title("Phát hiện mới về trí tuệ nhân tạo GPT")
                .summary("Nghiên cứu mới công bố trên tạp chí khoa học.")
                .url("https://vnexpress.net/ai-200")
                .source(vnExpress)
                .category(kinhDoanhCategory)
                .publishedAt(LocalDateTime.of(2026, 10, 3, 10, 0))
                .build();

        when(articleRepository.findById(200L)).thenReturn(Optional.of(singleArticle));
        when(articleRepository.findByPublishedAtBetween(any(), any()))
                .thenReturn(List.of(singleArticle));

        StoryTimelineResponse response = storyTimelineService.getTimelineForArticle(200L);

        assertThat(response).isNotNull();
        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().get(0).getArticleId()).isEqualTo(200L);
        assertThat(response.getEvents().get(0).getPhase()).isEqualTo(StoryTimelineService.PHASE_LATEST);
        assertThat(response.getEvents().get(0).getPhaseLabel()).isEqualTo(StoryTimelineService.LABEL_LATEST);
    }

    @Test
    void getTimelineForArticle_whenTwoArticles_shouldAssignGenesisAndLatest() {
        Article firstArticle = Article.builder()
                .id(301L)
                .title("Đoàn tàu Metro chuẩn bị vận hành thử nghiệm")
                .summary("Tuyến đường sắt đô thị bắt đầu chạy thử.")
                .url("https://tuoitre.vn/metro-301")
                .source(tuoiTre)
                .category(thoiSuCategory)
                .publishedAt(LocalDateTime.of(2026, 10, 1, 9, 0))
                .build();

        Article secondArticle = Article.builder()
                .id(302L)
                .title("Tuyến tàu Metro chính thức khai trương thương mại")
                .summary("Hàng ngàn người dân trải nghiệm ngày đầu tiên.")
                .url("https://vnexpress.net/metro-302")
                .source(vnExpress)
                .category(thoiSuCategory)
                .publishedAt(LocalDateTime.of(2026, 10, 3, 15, 0))
                .build();

        when(articleRepository.findById(302L)).thenReturn(Optional.of(secondArticle));
        when(articleRepository.findByPublishedAtBetween(any(), any()))
                .thenReturn(List.of(firstArticle, secondArticle));

        StoryTimelineResponse response = storyTimelineService.getTimelineForArticle(302L);

        assertThat(response).isNotNull();
        assertThat(response.getEvents()).hasSize(2);
        assertThat(eventsOf(response).get(0).getPhase()).isEqualTo(StoryTimelineService.PHASE_GENESIS);
        assertThat(eventsOf(response).get(1).getPhase()).isEqualTo(StoryTimelineService.PHASE_LATEST);
    }

    @Test
    void getTimelineForArticle_whenNotFound_shouldThrowResourceNotFoundException() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storyTimelineService.getTimelineForArticle(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Article not found with id: '999'");
    }

    private List<TimelineEventResponse> eventsOf(StoryTimelineResponse response) {
        return response.getEvents();
    }
}
