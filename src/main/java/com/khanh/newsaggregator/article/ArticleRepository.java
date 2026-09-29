package com.khanh.newsaggregator.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByUrlHash(String urlHash);

    Optional<Article> findByUrlHash(String urlHash);

    @Override
    @EntityGraph(attributePaths = {"source", "category"})
    Optional<Article> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"source", "category"})
    Page<Article> findAll(Pageable pageable);

    List<Article> findByPublishedAtAfter(java.time.LocalDateTime since);

    List<Article> findTop500ByOrderByPublishedAtDesc();

    @EntityGraph(attributePaths = {"source", "category"})
    Page<Article> findByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"source", "category"})
    Page<Article> findByCategorySlug(String categorySlug, Pageable pageable);

    @EntityGraph(attributePaths = {"source", "category"})
    List<Article> findTop10ByCategoryIdAndPublishedAtAfterOrderByPublishedAtDesc(Long categoryId, java.time.LocalDateTime since);

    @EntityGraph(attributePaths = {"source", "category"})
    List<Article> findTop10ByCategoryIdOrderByPublishedAtDesc(Long categoryId);

    // ==========================================
    // Full-Text Search Queries using TSVECTOR
    // ==========================================

    @Query(value = """
            SELECT a.* FROM article a
            WHERE a.search_vector @@ plainto_tsquery('simple', :q)
            ORDER BY ts_rank(a.search_vector, plainto_tsquery('simple', :q)) DESC, a.published_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM article a
            WHERE a.search_vector @@ plainto_tsquery('simple', :q)
            """,
            nativeQuery = true)
    Page<Article> searchArticles(@Param("q") String q, Pageable pageable);

    @Query(value = """
            SELECT a.* FROM article a
            JOIN category c ON a.category_id = c.id
            WHERE c.slug = :categorySlug
              AND a.search_vector @@ plainto_tsquery('simple', :q)
            ORDER BY ts_rank(a.search_vector, plainto_tsquery('simple', :q)) DESC, a.published_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM article a
            JOIN category c ON a.category_id = c.id
            WHERE c.slug = :categorySlug
              AND a.search_vector @@ plainto_tsquery('simple', :q)
            """,
            nativeQuery = true)
    Page<Article> searchArticlesByCategorySlug(@Param("categorySlug") String categorySlug, @Param("q") String q, Pageable pageable);

    @Query(value = """
            SELECT a.* FROM article a
            WHERE a.category_id = :categoryId
              AND a.search_vector @@ plainto_tsquery('simple', :q)
            ORDER BY ts_rank(a.search_vector, plainto_tsquery('simple', :q)) DESC, a.published_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM article a
            WHERE a.category_id = :categoryId
              AND a.search_vector @@ plainto_tsquery('simple', :q)
            """,
            nativeQuery = true)
    Page<Article> searchArticlesByCategoryId(@Param("categoryId") Long categoryId, @Param("q") String q, Pageable pageable);
}
