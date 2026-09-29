package com.khanh.newsaggregator.ingestion;

import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RssContentParser {

    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /**
     * Extracts an image URL from entry enclosure or description HTML.
     */
    public String extractImageUrl(SyndEntry entry) {
        // 1. Check enclosures for image types
        if (entry.getEnclosures() != null) {
            for (SyndEnclosure enc : entry.getEnclosures()) {
                if (enc.getType() != null && enc.getType().startsWith("image/")) {
                    return enc.getUrl();
                }
            }
        }

        // 2. Fallback: extract from <description> HTML
        String description = getRawDescription(entry);
        if (description != null) {
            Matcher matcher = IMG_SRC_PATTERN.matcher(description);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    /**
     * Clean HTML tags from description to produce a neat text summary.
     */
    public String cleanSummary(SyndEntry entry) {
        String description = getRawDescription(entry);
        if (description == null || description.isBlank()) {
            return null;
        }

        // Remove HTML tags
        String text = HTML_TAG_PATTERN.matcher(description).replaceAll("").trim();

        // Decode common HTML entities
        text = text.replace("&nbsp;", " ")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"")
                   .replace("&apos;", "'")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">");

        // Clean extra whitespace
        text = text.replaceAll("\\s+", " ").trim();

        return text.isBlank() ? null : text;
    }

    private String getRawDescription(SyndEntry entry) {
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            return entry.getDescription().getValue();
        }
        return null;
    }
}
