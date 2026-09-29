package com.khanh.newsaggregator.source;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByRssUrl(String rssUrl);

    List<Source> findByActiveTrue();
}
