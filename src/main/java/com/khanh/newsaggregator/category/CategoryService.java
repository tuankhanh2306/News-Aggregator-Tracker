package com.khanh.newsaggregator.category;

import com.khanh.newsaggregator.category.dto.CategoryResponse;
import com.khanh.newsaggregator.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = RedisConfig.CACHE_CATEGORIES)
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching categories from DATABASE (Cache Miss)");
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }
}
