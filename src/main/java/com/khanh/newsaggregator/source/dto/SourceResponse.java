package com.khanh.newsaggregator.source.dto;

import com.khanh.newsaggregator.source.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceResponse {

    private Long id;
    private String name;

    public static SourceResponse fromEntity(Source source) {
        if (source == null) {
            return null;
        }
        return SourceResponse.builder()
                .id(source.getId())
                .name(source.getName())
                .build();
    }
}
