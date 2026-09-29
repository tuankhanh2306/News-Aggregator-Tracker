package com.khanh.newsaggregator.common;

import com.khanh.newsaggregator.category.Category;
import com.khanh.newsaggregator.category.CategoryRepository;
import com.khanh.newsaggregator.source.Source;
import com.khanh.newsaggregator.source.SourceRepository;
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

    @Override
    public void run(ApplicationArguments args) {
        initCategories();
        initSources();
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
                        .name("Tuổi Trẻ - Tin mới nhất")
                        .rssUrl("https://tuoitre.vn/rss/tin-moi-nhat.rss")
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
                        .build(),
                Source.builder()
                        .name("Nguồn Lỗi Demo (Fault Isolation Test)")
                        .rssUrl("https://invalid-error-test-404-domain.vn/rss.xml")
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
}
