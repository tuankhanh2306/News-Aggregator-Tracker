package com.khanh.newsaggregator.article;

import com.khanh.newsaggregator.article.dto.ArticleResponse;
import com.khanh.newsaggregator.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<PageResponse<ArticleResponse>> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ArticleResponse> response = articleService.getArticles(category, categoryId, q, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Long id) {
        ArticleResponse article = articleService.getArticleById(id);
        return ResponseEntity.ok(article);
    }
}
