package com.khanh.newsaggregator.trending;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VietnameseKeywordExtractor {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "và", "của", "các", "những", "có", "là", "được", "một", "này", "khi", "cho",
            "với", "trong", "đã", "sẽ", "đang", "về", "ra", "ở", "tại", "từ", "sau", "đến",
            "nhiều", "lại", "vì", "sao", "làm", "gì", "bị", "để", "theo", "hơn", "người",
            "như", "nào", "cùng", "qua", "trên", "dưới", "giữa", "mới", "hôm", "nay", "ngày",
            "năm", "tháng", "giờ", "phút", "điểm", "tin", "thì", "mà", "nhưng", "dù", "nếu",
            "hay", "hoặc", "cả", "rồi", "lên", "xuống", "vào", "bởi", "do", "tự", "thể",
            "phải", "biết", "thấy", "nói", "rằng", "thêm", "bớt", "ít", "rất", "quá", "lắm",
            "khác", "khác nhau", "trước", "vẫn", "chỉ", "cũng", "chưa", "không", "chẳng", "đâu",
            "ai", "nào", "mỗi", "từng", "luôn", "ngay", "liền", "chính", "đây", "đó", "kia",
            "nơi", "chỗ", "việc", "sự", "cái", "con", "bộ", "loại", "bằng", "lúc", "khiến",
            "giúp", "mang", "đưa", "tìm", "xem", "cần", "muốn", "thành", "bắt", "đầu", "hết",
            "vừa", "tiếp", "tục", "toàn", "gần", "xa", "tới", "lần", "chiếc", "cuộc", "bài",
            "ông", "bà", "anh", "chị", "em", "vụ", "giá", "hàng", "nữ", "nam", "đội", "xe",
            "tiền", "thời", "cộng", "cách", "lý", "hội", "ngành", "loạt", "chi", "thu", "tỷ",
            "triệu", "đồng", "bán", "mua", "tăng", "giảm", "cao", "thấp", "nóng", "chuẩn",
            "báo", "ảnh", "video", "gặp", "đi", "khoảng", "nhất", "tuyển", "trận", "chơi",
            "quốc", "trung", "long", "bắc", "nam", "tây", "đông"
    ));

    // Pattern to match 2-3 capitalized words in sequence (e.g. "Hà Nội", "Việt Nam", "TP HCM", "Face ID")
    private static final Pattern MULTI_WORD_ENTITY_PATTERN =
            Pattern.compile("(?U)\\b(\\p{Lu}[\\p{L}\\p{Nd}]*(?:\\s+[\\p{Lu}\\p{Nd}][\\p{L}\\p{Nd}]*){1,2})\\b");

    // Pattern to match single capitalized or mixed-case entities (e.g. "Apple", "iPhone", "ChatGPT", "AI", "SJC")
    private static final Pattern SINGLE_ENTITY_PATTERN =
            Pattern.compile("(?U)\\b([a-zA-Z\\p{L}\\p{Nd}]+)\\b");

    /**
     * Extracts significant keywords and entities from an article title.
     *
     * @param title the article title
     * @return set of normalized keywords
     */
    public Set<String> extractKeywords(String title) {
        if (title == null || title.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> keywords = new LinkedHashSet<>();

        // 1. First extract multi-word capitalized named entities (e.g. "Việt Nam", "Hà Nội", "Thủ tướng")
        Matcher multiMatcher = MULTI_WORD_ENTITY_PATTERN.matcher(title);
        while (multiMatcher.find()) {
            String entity = multiMatcher.group(1).trim();
            keywords.add(entity);
        }

        // 2. Extract single distinctive words (brands, acronyms like AI, Apple, iPhone, SJC, NATO)
        Matcher singleMatcher = SINGLE_ENTITY_PATTERN.matcher(title);
        while (singleMatcher.find()) {
            String word = singleMatcher.group(1).trim();
            String lower = word.toLowerCase(Locale.ROOT);

            // Skip if it's a stopword, numeric only, or length < 2
            if (STOP_WORDS.contains(lower) || isAllDigits(word) || word.length() < 2) {
                continue;
            }

            // Keep acronyms (e.g. AI, USD, SJC, V-League)
            boolean isAllUpper = word.chars().allMatch(c -> !Character.isLetter(c) || Character.isUpperCase(c));
            // Keep camelCase / mixed words (e.g. iPhone, iPad, ChatGPT, TikTok)
            boolean isMixedCase = word.length() >= 4 && !word.equals(lower) && !isAllUpper;
            // Proper capitalized noun of length >= 4 (e.g. Apple, Google, Toyota, Donald, Trump)
            boolean isProperNoun = Character.isUpperCase(word.charAt(0)) && word.length() >= 4;

            if (isAllUpper || isMixedCase || isProperNoun) {
                // Ensure this word is not already a duplicate or sub-token of any multi-word entity
                boolean alreadyCovered = keywords.stream().anyMatch(k ->
                        k.equalsIgnoreCase(word) || Arrays.asList(k.split("\\s+")).contains(word)
                );
                if (!alreadyCovered) {
                    keywords.add(word);
                }
            }
        }

        return keywords;
    }

    private boolean isAllDigits(String s) {
        return s.chars().allMatch(Character::isDigit);
    }
}
