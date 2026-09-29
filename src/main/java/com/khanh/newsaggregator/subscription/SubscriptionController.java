package com.khanh.newsaggregator.subscription;

import com.khanh.newsaggregator.subscription.dto.SubscriptionRequest;
import com.khanh.newsaggregator.subscription.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(Principal principal) {
        List<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptions(principal.getName());
        return ResponseEntity.ok(subscriptions);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscriptionRequest request,
            Principal principal) {
        SubscriptionResponse response = subscriptionService.subscribe(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable Long categoryId,
            Principal principal) {
        subscriptionService.unsubscribe(principal.getName(), categoryId);
        return ResponseEntity.noContent().build();
    }
}
