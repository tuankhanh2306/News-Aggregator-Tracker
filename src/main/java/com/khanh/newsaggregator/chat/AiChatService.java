package com.khanh.newsaggregator.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.newsaggregator.article.Article;
import com.khanh.newsaggregator.article.ArticleRepository;
import com.khanh.newsaggregator.article.StoryTimelineService;
import com.khanh.newsaggregator.article.dto.StoryTimelineResponse;
import com.khanh.newsaggregator.article.dto.TimelineEventResponse;
import com.khanh.newsaggregator.chat.dto.ChatArticleRef;
import com.khanh.newsaggregator.chat.dto.ChatRequest;
import com.khanh.newsaggregator.chat.dto.ChatResponse;
import com.khanh.newsaggregator.trending.TrendingService;
import com.khanh.newsaggregator.trending.VietnameseKeywordExtractor;
import com.khanh.newsaggregator.trending.dto.TrendingKeywordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ArticleRepository articleRepository;
    private final StoryTimelineService storyTimelineService;
    private final TrendingService trendingService;
    private final VietnameseKeywordExtractor keywordExtractor;
    private final ObjectMapper objectMapper;

    @Value("${news.ai.gemini-api-key:}")
    private String geminiApiKey;

    @Value("${news.ai.gemini-model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${news.ai.groq-api-key:}")
    private String groqApiKey;

    @Value("${news.ai.groq-model:llama-3.3-70b-versatile}")
    private String groqModel;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private static final Set<String> QUESTION_FILLERS = new HashSet<>(Arrays.asList(
            "cho", "tôi", "mình", "bạn", "hỏi", "biết", "về", "tin", "tức", "bài", "báo",
            "hôm", "nay", "có", "gì", "nào", "mới", "nóng", "hot", "nhất", "tóm", "tắt",
            "giải", "thích", "phân", "tích", "diễn", "biến", "sự", "kiện", "này", "đang",
            "xem", "đọc", "là", "sao", "như", "thế", "vậy", "không", "những", "các", "của",
            "trong", "với", "theo", "từ", "đến", "điểm", "nhanh", "giúp", "ơi", "ai"
    ));

    public enum ChatIntent {
        CURRENT_ARTICLE,
        TIMELINE_ARC,
        HOT_BRIEFING,
        TRENDING_TOPICS,
        TOPIC_SEARCH
    }

    @Transactional(readOnly = true)
    public ChatResponse askAssistant(ChatRequest request) {
        String rawMessage = request.getMessage() != null ? request.getMessage().trim() : "";
        if (rawMessage.isEmpty()) {
            rawMessage = "Điểm tin nóng hôm nay";
        }

        // 1. Retrieve Current Article Context if user is viewing one
        Article currentArticle = null;
        StoryTimelineResponse timeline = null;
        if (request.getCurrentArticleId() != null) {
            currentArticle = articleRepository.findById(request.getCurrentArticleId()).orElse(null);
            if (currentArticle != null) {
                try {
                    timeline = storyTimelineService.getTimelineForArticle(currentArticle.getId());
                } catch (Exception e) {
                    log.debug("Could not build timeline for article {}: {}", currentArticle.getId(), e.getMessage());
                }
            }
        }

        // 2. Detect User Intent
        ChatIntent intent = detectIntent(rawMessage, currentArticle != null);

        // 3. Retrieve Relevant Articles Pool & Trending Keywords
        List<Article> recentPool = fetchRecentArticlesPool(request.getCategoryId());
        List<TrendingKeywordResponse> trendingKeywords = fetchSafeTrendingKeywords();
        List<Article> matchedArticles = selectRelevantArticles(rawMessage, intent, currentArticle, timeline, recentPool);

        // 4. Build Suggested Follow-up Prompts
        List<String> suggestedPrompts = buildSuggestedPrompts(intent, currentArticle, trendingKeywords);

        // 5. Convert Matched Articles to DTOs (up to 4 cards for clean UI)
        List<ChatArticleRef> articleRefs = matchedArticles.stream()
                .limit(4)
                .map(ChatArticleRef::fromEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 6. Try Cloud LLM (Gemini or Groq) if API Key is configured
        if (hasGeminiKey()) {
            String llmReply = callGeminiApi(rawMessage, currentArticle, timeline, matchedArticles, trendingKeywords);
            if (llmReply != null && !llmReply.isBlank()) {
                return ChatResponse.builder()
                        .reply(llmReply)
                        .engine("GEMINI")
                        .relatedArticles(articleRefs)
                        .suggestedPrompts(suggestedPrompts)
                        .build();
            }
        }

        if (hasGroqKey()) {
            String llmReply = callGroqApi(rawMessage, currentArticle, timeline, matchedArticles, trendingKeywords);
            if (llmReply != null && !llmReply.isBlank()) {
                return ChatResponse.builder()
                        .reply(llmReply)
                        .engine("GROQ")
                        .relatedArticles(articleRefs)
                        .suggestedPrompts(suggestedPrompts)
                        .build();
            }
        }

        // 7. Fallback / Default: Zero-Config Local Editorial RAG Synthesis
        String localReply = synthesizeLocalEditorialReply(
                rawMessage, intent, currentArticle, timeline, matchedArticles, recentPool, trendingKeywords
        );

        return ChatResponse.builder()
                .reply(localReply)
                .engine("LOCAL_EDITORIAL")
                .relatedArticles(articleRefs)
                .suggestedPrompts(suggestedPrompts)
                .build();
    }

    public Map<String, Object> getAssistantStatus() {
        String activeEngine = hasGeminiKey() ? "GEMINI" : (hasGroqKey() ? "GROQ" : "LOCAL_EDITORIAL");
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("activeEngine", activeEngine);
        status.put("cloudConfigured", hasGeminiKey() || hasGroqKey());
        status.put("defaultPrompts", Arrays.asList(
                "Tóm tắt tin đang xem",
                "Diễn biến sự kiện này",
                "Điểm 5 tin nóng nhất",
                "Chủ đề xu hướng hôm nay"
        ));
        return status;
    }

    public ChatIntent detectIntent(String message, boolean hasCurrentArticle) {
        String lower = message.toLowerCase(Locale.ROOT);

        // Check Timeline / Story Arc intent
        if (lower.contains("diễn biến") || lower.contains("dòng thời gian") || lower.contains("mạch sự kiện")
                || lower.contains("toàn cảnh") || lower.contains("trước đó") || lower.contains("bắt đầu từ")
                || lower.contains("nguồn gốc") || lower.contains("timeline")) {
            return hasCurrentArticle ? ChatIntent.TIMELINE_ARC : ChatIntent.HOT_BRIEFING;
        }

        // Check Current Article summary/analysis intent
        if (lower.contains("tin đang xem") || lower.contains("bài đang xem") || lower.contains("bài này")
                || lower.contains("tin này") || lower.contains("phân tích tin") || lower.contains("giải thích tin")
                || lower.contains("nội dung chính") || lower.contains("ý chính")) {
            return hasCurrentArticle ? ChatIntent.CURRENT_ARTICLE : ChatIntent.HOT_BRIEFING;
        }

        // Check Trending Topics intent
        if (lower.contains("xu hướng") || lower.contains("từ khóa") || lower.contains("trending")
                || lower.contains("chủ đề nổi bật") || lower.contains("quan tâm nhất") || lower.contains("hot trend")) {
            return ChatIntent.TRENDING_TOPICS;
        }

        // Check General Hot News Briefing intent
        if (lower.contains("điểm tin") || lower.contains("tin nóng") || lower.contains("tin mới nhất")
                || lower.contains("hôm nay có gì") || lower.contains("tin nổi bật") || lower.contains(" tin hot")
                || lower.equals("tóm tắt") || lower.equals("tin tức hôm nay")) {
            return ChatIntent.HOT_BRIEFING;
        }

        return ChatIntent.TOPIC_SEARCH;
    }

    private List<Article> fetchRecentArticlesPool(Long categoryId) {
        try {
            PageRequest pageRequest = PageRequest.of(0, 60, Sort.by(Sort.Direction.DESC, "publishedAt"));
            if (categoryId != null) {
                Page<Article> catPage = articleRepository.findByCategoryId(categoryId, pageRequest);
                if (catPage != null && !catPage.isEmpty()) {
                    return catPage.getContent();
                }
            }
            Page<Article> allPage = articleRepository.findAll(pageRequest);
            if (allPage != null && !allPage.isEmpty()) {
                return allPage.getContent();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch recent articles pool: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<TrendingKeywordResponse> fetchSafeTrendingKeywords() {
        try {
            List<TrendingKeywordResponse> kw = trendingService.getTrendingKeywords(8);
            return kw != null ? kw : Collections.emptyList();
        } catch (Exception e) {
            log.debug("Could not fetch trending keywords for chat context: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Article> selectRelevantArticles(
            String message,
            ChatIntent intent,
            Article currentArticle,
            StoryTimelineResponse timeline,
            List<Article> recentPool
    ) {
        if (intent == ChatIntent.CURRENT_ARTICLE && currentArticle != null) {
            List<Article> result = new ArrayList<>();
            result.add(currentArticle);
            if (timeline != null && timeline.getEvents() != null) {
                Set<Long> timelineIds = timeline.getEvents().stream()
                        .map(TimelineEventResponse::getArticleId)
                        .filter(id -> !Objects.equals(id, currentArticle.getId()))
                        .collect(Collectors.toSet());
                for (Article a : recentPool) {
                    if (timelineIds.contains(a.getId())) {
                        result.add(a);
                    }
                }
            }
            return result;
        }

        if (intent == ChatIntent.TIMELINE_ARC && currentArticle != null) {
            List<Article> result = new ArrayList<>();
            if (timeline != null && timeline.getEvents() != null) {
                Map<Long, Article> poolMap = recentPool.stream()
                        .filter(a -> a.getId() != null)
                        .collect(Collectors.toMap(Article::getId, a -> a, (a, b) -> a));
                if (currentArticle.getId() != null) {
                    poolMap.put(currentArticle.getId(), currentArticle);
                }
                for (TimelineEventResponse ev : timeline.getEvents()) {
                    Article a = poolMap.get(ev.getArticleId());
                    if (a != null) {
                        result.add(a);
                    }
                }
            }
            if (result.isEmpty()) {
                result.add(currentArticle);
            }
            return result;
        }

        if (intent == ChatIntent.HOT_BRIEFING || intent == ChatIntent.TRENDING_TOPICS) {
            return recentPool.stream().limit(5).collect(Collectors.toList());
        }

        // TOPIC_SEARCH: Score articles against extracted query tokens
        List<String> queryTerms = extractSearchTerms(message);
        if (queryTerms.isEmpty()) {
            return recentPool.stream().limit(4).collect(Collectors.toList());
        }

        List<Map.Entry<Article, Integer>> scored = new ArrayList<>();
        for (Article article : recentPool) {
            int score = scoreArticleMatch(article, queryTerms, message);
            if (score > 0) {
                scored.add(new AbstractMap.SimpleEntry<>(article, score));
            }
        }

        scored.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
        return scored.stream()
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<String> extractSearchTerms(String message) {
        String cleaned = message.replaceAll("[?.,!;:()\"'`~]", " ").trim();
        String[] tokens = cleaned.split("\\s+");
        List<String> terms = new ArrayList<>();
        for (String token : tokens) {
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.length() >= 2 && !QUESTION_FILLERS.contains(lower)) {
                terms.add(lower);
            }
        }
        return terms;
    }

    private int scoreArticleMatch(Article article, List<String> queryTerms, String rawQuery) {
        String title = article.getTitle() != null ? article.getTitle().toLowerCase(Locale.ROOT) : "";
        String summary = article.getSummary() != null ? article.getSummary().toLowerCase(Locale.ROOT) : "";
        String category = (article.getCategory() != null && article.getCategory().getName() != null)
                ? article.getCategory().getName().toLowerCase(Locale.ROOT) : "";

        int score = 0;
        String rawLower = rawQuery.toLowerCase(Locale.ROOT).trim();
        if (rawLower.length() >= 4 && title.contains(rawLower)) {
            score += 15;
        }

        for (String term : queryTerms) {
            if (title.contains(term)) {
                score += 5;
            }
            if (category.contains(term)) {
                score += 4;
            }
            if (summary.contains(term)) {
                score += 2;
            }
        }
        return score;
    }

    private String synthesizeLocalEditorialReply(
            String query,
            ChatIntent intent,
            Article currentArticle,
            StoryTimelineResponse timeline,
            List<Article> matchedArticles,
            List<Article> recentPool,
            List<TrendingKeywordResponse> trendingKeywords
    ) {
        StringBuilder sb = new StringBuilder();

        switch (intent) {
            case CURRENT_ARTICLE -> {
                if (currentArticle != null) {
                    String catName = (currentArticle.getCategory() != null && currentArticle.getCategory().getName() != null)
                            ? currentArticle.getCategory().getName() : "Thời sự";
                    String srcName = (currentArticle.getSource() != null && currentArticle.getSource().getName() != null)
                            ? currentArticle.getSource().getName() : "Báo điện tử";
                    String summary = (currentArticle.getSummary() != null && !currentArticle.getSummary().isBlank())
                            ? currentArticle.getSummary().trim()
                            : "Bản tin cập nhật trực tiếp từ " + srcName + ". Bạn có thể bấm vào thẻ bài viết bên dưới để đọc chi tiết toàn văn.";

                    sb.append("**Điểm cốt lõi bài đang đọc:**\n");
                    sb.append("• **Chủ đề:** ").append(catName).append(" (Nguồn: *").append(srcName).append("*)\n");
                    sb.append("• **Tóm tắt trọng tâm:** ").append(summary).append("\n");

                    Set<String> entities = keywordExtractor.extractKeywords(currentArticle.getTitle());
                    if (!entities.isEmpty()) {
                        sb.append("• **Thực thể / Từ khóa chính:** ")
                                .append(entities.stream().limit(5).map(e -> "`" + e + "`").collect(Collectors.joining(", ")))
                                .append("\n");
                    }

                    if (timeline != null && timeline.getEvents() != null && timeline.getEvents().size() > 1) {
                        sb.append("• **Mạch sự kiện:** Hệ thống phát hiện **")
                                .append(timeline.getEvents().size())
                                .append(" bài viết** liên quan trong chuỗi sự kiện này. Bạn có thể chọn *\"Diễn biến sự kiện này\"* để xem toàn cảnh từ lúc khởi nguồn.");
                    }
                    return sb.toString().trim();
                }
            }

            case TIMELINE_ARC -> {
                if (currentArticle != null && timeline != null && timeline.getEvents() != null && !timeline.getEvents().isEmpty()) {
                    sb.append("**Toàn cảnh dòng sự kiện (").append(timeline.getEvents().size()).append(" mốc thời gian):**\n");
                    List<TimelineEventResponse> events = timeline.getEvents();
                    int limit = Math.min(events.size(), 5);
                    for (int i = 0; i < limit; i++) {
                        TimelineEventResponse ev = events.get(i);
                        String timeStr = ev.getPublishedAt() != null ? ev.getPublishedAt().format(TIME_FMT) : "Cập nhật";
                        sb.append("• **[").append(ev.getPhaseLabel()).append(" - ").append(timeStr).append("]** ")
                                .append(ev.getTitle()).append(" (*").append(ev.getSourceName()).append("*)\n");
                    }
                    if (events.size() == 1) {
                        sb.append("Hiện đây là bản tin độc lập mới nhất về sự kiện này. Hệ thống sẽ tự động kết nối khi có các bài viết tiếp theo.");
                    }
                    return sb.toString().trim();
                }
            }

            case TRENDING_TOPICS -> {
                sb.append("**Tâm điểm xu hướng 24 giờ qua:**\n");
                if (!trendingKeywords.isEmpty()) {
                    String kwList = trendingKeywords.stream()
                            .limit(8)
                            .map(k -> "**#" + k.getKeyword() + "** (" + k.getCount() + " bài)")
                            .collect(Collectors.joining(" • "));
                    sb.append(kwList).append("\n\n");
                }
                if (!recentPool.isEmpty()) {
                    sb.append("**Các bản tin tiêu biểu đang được chú ý:**\n");
                    for (Article a : recentPool.stream().limit(3).toList()) {
                        sb.append("• ").append(a.getTitle());
                        if (a.getCategory() != null && a.getCategory().getName() != null) {
                            sb.append(" (*").append(a.getCategory().getName()).append("*)");
                        }
                        sb.append("\n");
                    }
                }
                return sb.toString().trim();
            }

            case HOT_BRIEFING -> {
                if (recentPool.isEmpty()) {
                    return "Hiện hệ thống đang đồng bộ dữ liệu RSS mới. Bạn vui lòng đợi ít giây rồi thử lại nhé!";
                }
                sb.append("**Điểm nhanh 5 bản tin đáng chú ý mới nhất:**\n");
                List<Article> top5 = recentPool.stream().limit(5).toList();
                for (Article a : top5) {
                    String cat = (a.getCategory() != null && a.getCategory().getName() != null)
                            ? "[" + a.getCategory().getName() + "] " : "";
                    sb.append("• **").append(cat).append(a.getTitle()).append("**");
                    if (a.getSummary() != null && !a.getSummary().isBlank()) {
                        String shortSum = a.getSummary().trim();
                        if (shortSum.length() > 110) {
                            shortSum = shortSum.substring(0, 107) + "...";
                        }
                        sb.append(" — ").append(shortSum);
                    }
                    sb.append("\n");
                }
                return sb.toString().trim();
            }

            default -> {
                // TOPIC_SEARCH
            }
        }

        // Handle TOPIC_SEARCH or fallback when currentArticle is null
        if (!matchedArticles.isEmpty()) {
            sb.append("**Tổng hợp nhanh về \"").append(query).append("\" (")
                    .append(matchedArticles.size()).append(" bài liên quan):**\n");
            for (Article a : matchedArticles.stream().limit(4).toList()) {
                String src = (a.getSource() != null && a.getSource().getName() != null)
                        ? a.getSource().getName() : "Báo điện tử";
                sb.append("• **").append(a.getTitle()).append("** (*").append(src).append("*)\n");
                if (a.getSummary() != null && !a.getSummary().isBlank()) {
                    String s = a.getSummary().trim();
                    if (s.length() > 130) {
                        s = s.substring(0, 127) + "...";
                    }
                    sb.append("  ").append(s).append("\n");
                }
            }
            return sb.toString().trim();
        }

        // No exact match found -> offer helpful editorial guidance + latest headlines
        sb.append("Chưa tìm thấy bản tin khớp chính xác với cụm từ **\"").append(query).append("\"** trong đợt cập nhật gần nhất.\n\n");
        if (!trendingKeywords.isEmpty()) {
            String kw = trendingKeywords.stream().limit(5).map(k -> "`#" + k.getKeyword() + "`").collect(Collectors.joining(", "));
            sb.append("• **Từ khóa đang nổi bật:** ").append(kw).append("\n");
        }
        if (!recentPool.isEmpty()) {
            sb.append("• **Gợi ý tin mới vừa lên trang:**\n");
            for (Article a : recentPool.stream().limit(3).toList()) {
                sb.append("  - ").append(a.getTitle()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private List<String> buildSuggestedPrompts(
            ChatIntent intent,
            Article currentArticle,
            List<TrendingKeywordResponse> trendingKeywords
    ) {
        List<String> prompts = new ArrayList<>();
        if (currentArticle != null) {
            if (intent != ChatIntent.CURRENT_ARTICLE) {
                prompts.add("Tóm tắt tin đang xem");
            }
            if (intent != ChatIntent.TIMELINE_ARC) {
                prompts.add("Diễn biến sự kiện này");
            }
        }
        if (intent != ChatIntent.HOT_BRIEFING) {
            prompts.add("Điểm 5 tin nóng nhất");
        }
        if (intent != ChatIntent.TRENDING_TOPICS) {
            prompts.add("Chủ đề xu hướng hôm nay");
        }
        if (!trendingKeywords.isEmpty()) {
            String topKw = trendingKeywords.get(0).getKeyword();
            prompts.add("Tin mới về " + topKw);
        }
        return prompts.stream().distinct().limit(4).collect(Collectors.toList());
    }

    private boolean hasGeminiKey() {
        return geminiApiKey != null && !geminiApiKey.trim().isEmpty();
    }

    private boolean hasGroqKey() {
        return groqApiKey != null && !groqApiKey.trim().isEmpty();
    }

    private String buildRagContextPrompt(
            String userQuestion,
            Article currentArticle,
            StoryTimelineResponse timeline,
            List<Article> matchedArticles,
            List<TrendingKeywordResponse> trendingKeywords
    ) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Bạn là Trợ lý Biên tập AI của VNNews Hub. Hãy trả lời câu hỏi của độc giả bằng tiếng Việt súc tích, rõ ràng, khách quan (dùng gạch đầu dòng • và in đậm **từ khóa** quan trọng, tối đa 160 từ). Chỉ sử dụng thông tin từ ngữ cảnh bản tin dưới đây:\n\n");

        if (currentArticle != null) {
            ctx.append("[BÀI BÁO ĐỘC GIẢ ĐANG XEM]\n");
            ctx.append("- Tiêu đề: ").append(currentArticle.getTitle()).append("\n");
            if (currentArticle.getSummary() != null) {
                ctx.append("- Tóm tắt: ").append(currentArticle.getSummary()).append("\n");
            }
            if (timeline != null && timeline.getEvents() != null && timeline.getEvents().size() > 1) {
                ctx.append("- Chuỗi diễn biến liên quan:\n");
                for (TimelineEventResponse ev : timeline.getEvents()) {
                    ctx.append("  * [").append(ev.getPhaseLabel()).append("] ").append(ev.getTitle()).append("\n");
                }
            }
            ctx.append("\n");
        }

        if (!matchedArticles.isEmpty()) {
            ctx.append("[CÁC BẢN TIN LIÊN QUAN TRONG HỆ THỐNG]\n");
            for (Article a : matchedArticles.stream().limit(5).toList()) {
                ctx.append("- ").append(a.getTitle());
                if (a.getSummary() != null && !a.getSummary().isBlank()) {
                    ctx.append(": ").append(a.getSummary());
                }
                ctx.append("\n");
            }
            ctx.append("\n");
        }

        if (!trendingKeywords.isEmpty()) {
            ctx.append("[TỪ KHÓA XU HƯỚNG 24H]: ")
                    .append(trendingKeywords.stream().map(TrendingKeywordResponse::getKeyword).collect(Collectors.joining(", ")))
                    .append("\n\n");
        }

        ctx.append("CÂU HỎI CỦA ĐỘC GIẢ: ").append(userQuestion);
        return ctx.toString();
    }

    private String callGeminiApi(
            String userQuestion,
            Article currentArticle,
            StoryTimelineResponse timeline,
            List<Article> matchedArticles,
            List<TrendingKeywordResponse> trendingKeywords
    ) {
        try {
            String prompt = buildRagContextPrompt(userQuestion, currentArticle, timeline, matchedArticles, trendingKeywords);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiModel.trim() + ":generateContent?key=" + geminiApiKey.trim();

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.3,
                            "maxOutputTokens", 450
                    )
            );

            String bodyJson = objectMapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(res.body());
                JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
                    return textNode.asText().trim();
                }
            } else {
                log.warn("Gemini API returned status {}: {}", res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.warn("Gemini API call failed, falling back to Local Editorial RAG: {}", e.getMessage());
        }
        return null;
    }

    private String callGroqApi(
            String userQuestion,
            Article currentArticle,
            StoryTimelineResponse timeline,
            List<Article> matchedArticles,
            List<TrendingKeywordResponse> trendingKeywords
    ) {
        try {
            String prompt = buildRagContextPrompt(userQuestion, currentArticle, timeline, matchedArticles, trendingKeywords);
            String url = "https://api.groq.com/openai/v1/chat/completions";

            Map<String, Object> payload = Map.of(
                    "model", groqModel.trim(),
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3,
                    "max_tokens", 450
            );

            String bodyJson = objectMapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(res.body());
                JsonNode textNode = root.path("choices").path(0).path("message").path("content");
                if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
                    return textNode.asText().trim();
                }
            } else {
                log.warn("Groq API returned status {}: {}", res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.warn("Groq API call failed, falling back to Local Editorial RAG: {}", e.getMessage());
        }
        return null;
    }
}
