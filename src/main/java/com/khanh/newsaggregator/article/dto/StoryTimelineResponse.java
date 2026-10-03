package com.khanh.newsaggregator.article.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryTimelineResponse {

    private Long articleId;
    private String topicTitle;

    @Builder.Default
    private List<TimelineEventResponse> events = new ArrayList<>();

    @JsonProperty("timelineEvents")
    public List<TimelineEventResponse> getTimelineEvents() {
        return events != null ? events : List.of();
    }
}
