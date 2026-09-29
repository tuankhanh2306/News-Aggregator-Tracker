package com.khanh.newsaggregator.subscription.dto;

import com.khanh.newsaggregator.subscription.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String frequency;
    private LocalDateTime createdAt;

    public static SubscriptionResponse fromEntity(Subscription subscription) {
        return SubscriptionResponse.builder()
                .categoryId(subscription.getCategory().getId())
                .categoryName(subscription.getCategory().getName())
                .categorySlug(subscription.getCategory().getSlug())
                .frequency(subscription.getFrequency())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
