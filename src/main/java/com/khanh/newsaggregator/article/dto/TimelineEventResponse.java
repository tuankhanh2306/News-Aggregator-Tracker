package com.khanh.newsaggregator.article.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventResponse {

    private Long articleId;
    private String title;
    private String url;
    private String sourceName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    private String phase; // GENESIS, PROGRESSION, LATEST
    private String phaseLabel; // "Khởi nguồn", "Diễn biến", "Mới nhất"
}
