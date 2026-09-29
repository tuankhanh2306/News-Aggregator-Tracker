package com.khanh.newsaggregator.trending;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VietnameseKeywordExtractorTest {

    private VietnameseKeywordExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new VietnameseKeywordExtractor();
    }

    @Test
    void extractKeywords_shouldExtractNamedEntitiesAndFilterStopwords() {
        String title = "Apple ra iOS 27.0.1 vá lỗi Face ID cho iPhone 18 Pro tại Hà Nội";
        Set<String> keywords = extractor.extractKeywords(title);

        assertTrue(keywords.stream().anyMatch(k -> k.equalsIgnoreCase("Apple")));
        assertTrue(keywords.contains("Hà Nội"));
        assertTrue(keywords.stream().anyMatch(k -> k.toLowerCase().contains("iphone")));

        // Verify stopwords like "cho", "tại", "ra" are NOT present
        assertFalse(keywords.contains("cho"));
        assertFalse(keywords.contains("tại"));
        assertFalse(keywords.contains("ra"));
    }

    @Test
    void extractKeywords_emptyTitle_shouldReturnEmptySet() {
        assertTrue(extractor.extractKeywords(null).isEmpty());
        assertTrue(extractor.extractKeywords("   ").isEmpty());
    }
}
