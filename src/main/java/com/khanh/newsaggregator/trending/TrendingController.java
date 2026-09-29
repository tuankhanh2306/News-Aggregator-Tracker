package com.khanh.newsaggregator.trending;

import com.khanh.newsaggregator.trending.dto.TrendingKeywordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping("/keywords")
    public ResponseEntity<List<TrendingKeywordResponse>> getTrendingKeywords(
            @RequestParam(defaultValue = "10") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<TrendingKeywordResponse> keywords = trendingService.getTrendingKeywords(safeLimit);
        return ResponseEntity.ok(keywords);
    }
}
