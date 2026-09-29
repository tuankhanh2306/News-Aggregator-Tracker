package com.khanh.newsaggregator.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Builder.Default
    private String frequency = "DAILY";
}
