package com.khanh.newsaggregator.ingestion;

import com.khanh.newsaggregator.article.Article;
import com.khanh.newsaggregator.article.ArticleRepository;
import com.khanh.newsaggregator.category.Category;
import com.khanh.newsaggregator.category.CategoryRepository;
import com.khanh.newsaggregator.common.util.HashUtils;
import com.khanh.newsaggregator.source.Source;
import com.khanh.newsaggregator.source.SourceRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssIngestionService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 10000;

    private final ArticleRepository articleRepository;
    private final SourceRepository sourceRepository;
    private final CategoryRepository categoryRepository;
    private final RssContentParser contentParser;

    /**
     * Ingest articles from a single source.
     *
     * @param source the source to fetch
     * @return number of new articles inserted
     */
    @Transactional
    public int ingestSource(Source source) {
        log.info("Fetching RSS from source [id={}, name='{}', url='{}']", source.getId(), source.getName(), source.getRssUrl());
        try {
            SyndFeed feed = fetchFeed(source.getRssUrl());
            List<Article> newArticles = new ArrayList<>();

            for (SyndEntry entry : feed.getEntries()) {
                String link = entry.getLink();
                if (link == null || link.isBlank()) {
                    continue;
                }

                String urlHash = HashUtils.sha256(link);
                if (articleRepository.existsByUrlHash(urlHash)) {
                    // Already ingested, skip to prevent duplicates
                    continue;
                }

                String title = truncate(entry.getTitle() != null ? entry.getTitle().trim() : "Không có tiêu đề", 500);
                String summary = contentParser.cleanSummary(entry);
                String imageUrl = truncate(contentParser.extractImageUrl(entry), 1000);
                LocalDateTime publishedAt = toLocalDateTime(entry.getPublishedDate());
                Category category = determineCategory(title, summary);

                Article article = Article.builder()
                        .source(source)
                        .category(category)
                        .title(title)
                        .summary(summary)
                        .url(truncate(link, 1000))
                        .urlHash(urlHash)
                        .imageUrl(imageUrl)
                        .publishedAt(publishedAt)
                        .fetchedAt(LocalDateTime.now())
                        .build();

                newArticles.add(article);
            }

            if (!newArticles.isEmpty()) {
                articleRepository.saveAll(newArticles);
                log.info("Source [{}]: Saved {} new articles", source.getName(), newArticles.size());
            } else {
                log.info("Source [{}]: No new articles found", source.getName());
            }

            source.setLastFetchedAt(LocalDateTime.now());
            sourceRepository.save(source);

            return newArticles.size();
        } catch (Exception e) {
            log.error("Failed to ingest RSS from source [{}]: {}", source.getName(), e.getMessage(), e);
            throw new RuntimeException("Ingestion error for source " + source.getName(), e);
        }
    }

    private SyndFeed fetchFeed(String rssUrl) throws Exception {
        URLConnection connection = URI.create(rssUrl).toURL().openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);

        try (InputStream inputStream = connection.getInputStream()) {
            return new SyndFeedInput().build(new XmlReader(inputStream));
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public Category determineCategory(String title, String summary) {
        String text = ((title != null ? title : "") + " " + (summary != null ? summary : "")).toLowerCase();

        // 1. Công nghệ
        if (text.contains("ai") || text.contains("công nghệ") || text.contains("chip") || text.contains("bán dẫn")
                || text.contains("smartphone") || text.contains("phần mềm") || text.contains("hacker")
                || text.contains("internet") || text.contains("google") || text.contains("apple")
                || text.contains("openai") || text.contains("robot") || text.contains("máy tính")
                || text.contains("samsung") || text.contains("nvidia")) {
            return categoryRepository.findBySlug("cong-nghe").orElse(null);
        }

        // 2. Kinh doanh
        if (text.contains("chứng khoán") || text.contains("giá vàng") || text.contains("bất động sản")
                || text.contains("ngân hàng") || text.contains("lãi suất") || text.contains("usd")
                || text.contains("doanh nghiệp") || text.contains("thị trường") || text.contains("tài chính")
                || text.contains("xuất khẩu") || text.contains("giá xăng") || text.contains("lạm phát")
                || text.contains("đầu tư") || text.contains("kinh tế") || text.contains("doanh thu")
                || text.contains("cổ phiếu") || text.contains("tỷ phú")) {
            return categoryRepository.findBySlug("kinh-doanh").orElse(null);
        }

        // 3. Thế giới
        if (text.contains("mỹ") || text.contains("nga") || text.contains("ukraine") || text.contains("trung quốc")
                || text.contains("israel") || text.contains("thế giới") || text.contains("quốc tế")
                || text.contains("gaza") || text.contains("liên hợp quốc") || text.contains("châu âu")
                || text.contains("nato") || text.contains("nhật bản") || text.contains("hàn quốc")
                || text.contains("tổng thống") || text.contains("ngoại giao") || text.contains("iran")) {
            return categoryRepository.findBySlug("the-gioi").orElse(null);
        }

        // 4. Mặc định: Thời sự (cho các tin xã hội, giao thông, đời sống, pháp luật...)
        return categoryRepository.findBySlug("thoi-su").orElse(null);
    }
}
