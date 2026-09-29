package com.khanh.newsaggregator.digest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDigestDto {

    private String categoryName;
    private List<ArticleDigestDto> articles;
}
