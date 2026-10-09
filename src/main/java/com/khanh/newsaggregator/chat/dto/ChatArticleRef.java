package com.khanh.newsaggregator.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.khanh.newsaggregator.article.Article;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatArticleRef {

    private Long id;
    private String title;
    private String summary;
    private String url;
    private String imageUrl;
    private String sourceName;
    private String categoryName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    public static ChatArticleRef fromEntity(Article article) {
        if (article == null) {
            return null;
        }
        String srcName = (article.getSource() != null && article.getSource().getName() != null)
                ? article.getSource().getName()
                : "Báo điện tử";
        String catName = (article.getCategory() != null && article.getCategory().getName() != null)
                ? article.getCategory().getName()
                : "Tin tức";

        return ChatArticleRef.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .url(article.getUrl())
                .imageUrl(article.getImageUrl())
                .sourceName(srcName)
                .categoryName(catName)
                .publishedAt(article.getPublishedAt() != null ? article.getPublishedAt() : article.getFetchedAt())
                .build();
    }
}
