package com.khanh.newsaggregator;

import com.khanh.newsaggregator.article.Article;
import com.khanh.newsaggregator.article.ArticleRepository;
import com.khanh.newsaggregator.category.Category;
import com.khanh.newsaggregator.category.CategoryRepository;
import com.khanh.newsaggregator.common.util.HashUtils;
import com.khanh.newsaggregator.source.Source;
import com.khanh.newsaggregator.source.SourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ArticleIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testFullTextSearchOnRealPostgresContainer() {
        // 1. Prepare Source and Category
        Source source = sourceRepository.save(Source.builder()
                .name("Test Source")
                .rssUrl("https://test.com/rss-" + System.currentTimeMillis())
                .active(true)
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("AI & Tech")
                .slug("ai-tech-" + System.currentTimeMillis())
                .build());

        // 2. Insert Article into PostgreSQL Testcontainer
        String url = "https://test.com/article-" + System.currentTimeMillis();
        Article article = Article.builder()
                .title("Trí tuệ nhân tạo AI và sự phát triển vượt bậc của công nghệ")
                .summary("Tổng quan về các mô hình ngôn ngữ lớn LLM và tương lai AI")
                .url(url)
                .urlHash(HashUtils.sha256(url))
                .source(source)
                .category(category)
                .publishedAt(LocalDateTime.now())
                .build();

        article = articleRepository.save(article);
        assertNotNull(article.getId());

        // 3. Perform Full-Text Search via TSVECTOR and plainto_tsquery
        Page<Article> searchResults = articleRepository.searchArticles("nhân tạo", PageRequest.of(0, 10));

        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.getContent().stream()
                .anyMatch(a -> a.getTitle().contains("nhân tạo")));
    }
}
