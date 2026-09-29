package com.khanh.newsaggregator.subscription;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {

    @EntityGraph(attributePaths = {"category"})
    List<Subscription> findWithCategoryByUserId(Long userId);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.user JOIN FETCH s.category")
    List<Subscription> findAllWithUserAndCategory();

    boolean existsById(SubscriptionId id);

    void deleteById(SubscriptionId id);
}
