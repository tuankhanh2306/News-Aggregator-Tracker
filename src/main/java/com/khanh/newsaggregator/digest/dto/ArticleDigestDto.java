package com.khanh.newsaggregator.digest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDigestDto {

    private String title;
    private String summary;
    private String url;
    private String sourceName;
    private LocalDateTime publishedAt;
}
