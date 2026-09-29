package com.khanh.newsaggregator.article;

import com.khanh.newsaggregator.article.dto.ArticleResponse;
import com.khanh.newsaggregator.common.dto.PageResponse;
import com.khanh.newsaggregator.common.exception.ResourceNotFoundException;
import com.khanh.newsaggregator.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Cacheable(
            value = RedisConfig.CACHE_ARTICLES,
            key = "{#categorySlug, #categoryId, #q, #pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString()}"
    )
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getArticles(String categorySlug, Long categoryId, String q, Pageable pageable) {
        log.info("Fetching articles from DATABASE (Cache Miss): categorySlug={}, categoryId={}, q={}, page={}",
                categorySlug, categoryId, q, pageable.getPageNumber());

        Page<Article> articlePage;

        boolean hasQuery = q != null && !q.isBlank();
        boolean hasCategorySlug = categorySlug != null && !categorySlug.isBlank();
        boolean hasCategoryId = categoryId != null;

        if (hasQuery) {
            Pageable searchPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            String query = q.trim();

            if (hasCategorySlug) {
                articlePage = articleRepository.searchArticlesByCategorySlug(categorySlug, query, searchPageable);
            } else if (hasCategoryId) {
                articlePage = articleRepository.searchArticlesByCategoryId(categoryId, query, searchPageable);
            } else {
                articlePage = articleRepository.searchArticles(query, searchPageable);
            }
        } else if (hasCategorySlug) {
            articlePage = articleRepository.findByCategorySlug(categorySlug, pageable);
        } else if (hasCategoryId) {
            articlePage = articleRepository.findByCategoryId(categoryId, pageable);
        } else {
            articlePage = articleRepository.findAll(pageable);
        }

        Page<ArticleResponse> responsePage = articlePage.map(ArticleResponse::fromEntity);
        return PageResponse.of(responsePage);
    }

    @Cacheable(value = RedisConfig.CACHE_ARTICLE_DETAIL, key = "#id")
    @Transactional(readOnly = true)
    public ArticleResponse getArticleById(Long id) {
        log.info("Fetching article detail from DATABASE (Cache Miss) for id={}", id);
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", id));
        return ArticleResponse.fromEntity(article);
    }

    /**
     * Clear articles cache when new articles are ingested.
     */
    @CacheEvict(value = RedisConfig.CACHE_ARTICLES, allEntries = true)
    public void evictArticlesCache() {
        log.info("Evicted all articles cache in Redis (Cache Invalidation)");
    }
}
