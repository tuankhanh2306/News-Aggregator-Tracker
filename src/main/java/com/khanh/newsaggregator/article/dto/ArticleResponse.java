package com.khanh.newsaggregator.article.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.khanh.newsaggregator.article.Article;
import com.khanh.newsaggregator.category.dto.CategoryResponse;
import com.khanh.newsaggregator.source.dto.SourceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {

    private Long id;
    private String title;
    private String summary;
    private String url;
    private String imageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fetchedAt;

    private SourceResponse source;
    private CategoryResponse category;

    public static ArticleResponse fromEntity(Article article) {
        if (article == null) {
            return null;
        }
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .url(article.getUrl())
                .imageUrl(article.getImageUrl())
                .publishedAt(article.getPublishedAt())
                .fetchedAt(article.getFetchedAt())
                .source(SourceResponse.fromEntity(article.getSource()))
                .category(CategoryResponse.fromEntity(article.getCategory()))
                .build();
    }
}
