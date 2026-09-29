package com.khanh.newsaggregator.subscription;

import com.khanh.newsaggregator.category.Category;
import com.khanh.newsaggregator.category.CategoryRepository;
import com.khanh.newsaggregator.common.exception.ResourceNotFoundException;
import com.khanh.newsaggregator.subscription.dto.SubscriptionRequest;
import com.khanh.newsaggregator.subscription.dto.SubscriptionResponse;
import com.khanh.newsaggregator.user.AppUser;
import com.khanh.newsaggregator.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptions(String userEmail) {
        AppUser user = getUserByEmail(userEmail);
        return subscriptionRepository.findWithCategoryByUserId(user.getId()).stream()
                .map(SubscriptionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubscriptionResponse subscribe(String userEmail, SubscriptionRequest request) {
        AppUser user = getUserByEmail(userEmail);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        SubscriptionId id = new SubscriptionId(user.getId(), category.getId());
        if (subscriptionRepository.existsById(id)) {
            throw new IllegalArgumentException("Already subscribed to category: " + category.getName());
        }

        Subscription subscription = Subscription.builder()
                .id(id)
                .user(user)
                .category(category)
                .frequency(request.getFrequency() != null ? request.getFrequency().toUpperCase() : "DAILY")
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("User {} subscribed to category {}", user.getEmail(), category.getName());

        return SubscriptionResponse.fromEntity(subscription);
    }

    @Transactional
    public void unsubscribe(String userEmail, Long categoryId) {
        AppUser user = getUserByEmail(userEmail);
        SubscriptionId id = new SubscriptionId(user.getId(), categoryId);

        if (!subscriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subscription not found for category id: " + categoryId);
        }

        subscriptionRepository.deleteById(id);
        log.info("User {} unsubscribed from category id {}", user.getEmail(), categoryId);
    }

    private AppUser getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
