package com.khanh.newsaggregator.common;

import com.khanh.newsaggregator.article.Article;
import com.khanh.newsaggregator.article.ArticleRepository;
import com.khanh.newsaggregator.category.Category;
import com.khanh.newsaggregator.category.CategoryRepository;
import com.khanh.newsaggregator.ingestion.RssIngestionService;
import com.khanh.newsaggregator.source.Source;
import com.khanh.newsaggregator.source.SourceRepository;
import com.khanh.newsaggregator.trending.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final SourceRepository sourceRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final RssIngestionService rssIngestionService;
    private final TrendingService trendingService;

    @Override
    public void run(ApplicationArguments args) {
        initCategories();
        initSources();
        backfillCategoriesIfNull();
        triggerInitialIngestionIfEmpty();
    }

    private void initCategories() {
        if (categoryRepository.count() == 0) {
            log.info("Seeding default categories...");
            categoryRepository.save(Category.builder().name("Thời sự").slug("thoi-su").build());
            categoryRepository.save(Category.builder().name("Kinh doanh").slug("kinh-doanh").build());
            categoryRepository.save(Category.builder().name("Công nghệ").slug("cong-nghe").build());
            categoryRepository.save(Category.builder().name("Thế giới").slug("the-gioi").build());
        }
    }

    private void initSources() {
        List<Source> sourcesToSeed = List.of(
                Source.builder()
                        .name("VnExpress - Tin mới nhất")
                        .rssUrl("https://vnexpress.net/rss/tin-moi-nhat.rss")
                        .active(true)
                        .build(),
                Source.builder()
                        .name("VnExpress - Thời sự")
                        .rssUrl("https://vnexpress.net/rss/thoi-su.rss")
                        .active(true)
                        .build(),
                Source.builder()
                        .name("VnExpress - Kinh doanh")
                        .rssUrl("https://vnexpress.net/rss/kinh-doanh.rss")
                        .active(true)
                        .build(),
                Source.builder()
                        .name("VnExpress - Khoa học công nghệ")
                        .rssUrl("https://vnexpress.net/rss/khoa-hoc-cong-nghe.rss")
                        .active(true)
                        .build(),
                Source.builder()
                        .name("VnExpress - Thế giới")
                        .rssUrl("https://vnexpress.net/rss/the-gioi.rss")
                        .active(true)
                        .build(),
                Source.builder()
                        .name("Tuổi Trẻ - Tin mới nhất")
                        .rssUrl("https://tuoitre.vn/rss/tin-moi-nhat.rss")
                        .active(true)
                        .build(),
                Source.builder()
                        .name("Tuổi Trẻ - Công nghệ Nhịp Sống Số")
                        .rssUrl("https://tuoitre.vn/rss/nhip-song-so.rss")
                        .active(true)
                        .build(),
                Source.builder()
                        .name("Thanh Niên - Trang chủ")
                        .rssUrl("https://thanhnien.vn/rss/home.rss")
                        .active(true)
                        .build(),
                Source.builder()
                        .name("Dân Trí - Sự kiện")
                        .rssUrl("https://dantri.com.vn/rss/su-kien.rss")
                        .active(true)
                        .build()
        );

        for (Source source : sourcesToSeed) {
            if (sourceRepository.findByRssUrl(source.getRssUrl()).isEmpty()) {
                sourceRepository.save(source);
                log.info("Seeded RSS source: {}", source.getName());
            }
        }
    }

    private void backfillCategoriesIfNull() {
        try {
            List<Article> allArticles = articleRepository.findAll();
            boolean hasUpdates = false;
            for (Article a : allArticles) {
                if (a.getCategory() == null) {
                    Category cat = rssIngestionService.determineCategory(a.getTitle(), a.getSummary());
                    if (cat != null) {
                        a.setCategory(cat);
                        hasUpdates = true;
                    }
                }
            }
            if (hasUpdates) {
                articleRepository.saveAll(allArticles);
                log.info("Backfilled categories for {} articles.", allArticles.size());
            }
        } catch (Exception e) {
            log.warn("Could not backfill categories: {}", e.getMessage());
        }
    }

    private void triggerInitialIngestionIfEmpty() {
        long count = articleRepository.count();
        if (count == 0) {
            log.info("Article database is currently empty. Running initial RSS ingestion on startup...");
            List<Source> sources = sourceRepository.findByActiveTrue();
            for (Source s : sources) {
                try {
                    int inserted = rssIngestionService.ingestSource(s);
                    log.info("Initial ingestion source [{}]: inserted {} articles", s.getName(), inserted);
                } catch (Exception e) {
                    log.warn("Initial ingestion skipped source {}: {}", s.getName(), e.getMessage());
                }
            }
            try {
                trendingService.calculateAndSaveTrending(24, 20);
                log.info("Calculated initial trending keywords.");
            } catch (Exception e) {
                log.warn("Initial trending calculation warning: {}", e.getMessage());
            }
        }
    }
}
