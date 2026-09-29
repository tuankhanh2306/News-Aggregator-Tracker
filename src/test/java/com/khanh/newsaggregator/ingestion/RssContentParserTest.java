package com.khanh.newsaggregator.ingestion;

import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RssContentParserTest {

    private RssContentParser parser;

    @BeforeEach
    void setUp() {
        parser = new RssContentParser();
    }

    @Test
    void extractImageUrl_fromDescriptionImgTag_shouldReturnSrc() {
        SyndEntryImpl entry = new SyndEntryImpl();
        SyndContentImpl description = new SyndContentImpl();
        description.setValue("<a href=\"https://vnexpress.net\"><img src=\"https://i1-vnexpress.vnecdn.net/2026/test.jpg\"></a> Ban tin nhanh.");
        entry.setDescription(description);

        String imageUrl = parser.extractImageUrl(entry);
        assertEquals("https://i1-vnexpress.vnecdn.net/2026/test.jpg", imageUrl);
    }

    @Test
    void cleanSummary_shouldStripHtmlTagsAndTrim() {
        SyndEntryImpl entry = new SyndEntryImpl();
        SyndContentImpl description = new SyndContentImpl();
        description.setValue("<a href=\"...\"><img src=\"...\"></a>&quot;Hà Nội&quot; &amp; TP.HCM mưa rào rải rác.&nbsp;");
        entry.setDescription(description);

        String summary = parser.cleanSummary(entry);
        assertEquals("\"Hà Nội\" & TP.HCM mưa rào rải rác.", summary);
    }
}
