package com.khanh.newsaggregator.trending;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KeywordStatRepository extends JpaRepository<KeywordStat, Long> {

    @Query("""
            SELECT k FROM KeywordStat k
            WHERE k.windowStart >= :since
            ORDER BY k.count DESC
            """)
    List<KeywordStat> findTopKeywordsSince(@Param("since") LocalDateTime since);

    List<KeywordStat> findByWindowStartOrderByCountDesc(LocalDateTime windowStart);
}
