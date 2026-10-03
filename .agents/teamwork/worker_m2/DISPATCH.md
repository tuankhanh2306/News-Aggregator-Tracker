## 2026-10-03T15:13:53Z

You are Worker 2 (teamwork_preview_worker) for Milestone M2 (R3: Algorithmic Core Backend & Frontend).

Working directory: e:\News\News-Aggregator-Tracker\.agents\teamwork\worker_m2
Authoritative user request: e:\News\News-Aggregator-Tracker\.agents\teamwork\ORIGINAL_REQUEST.md
Project plan: e:\News\News-Aggregator-Tracker\.agents\teamwork\PROJECT.md
Audit Report: e:\News\News-Aggregator-Tracker\DEEP_UX_AUDIT_REPORT.md

MANDATORY FIRST STEP: Read e:\News\News-Aggregator-Tracker\.agents\teamwork\ORIGINAL_REQUEST.md and e:\News\News-Aggregator-Tracker\.agents\teamwork\PROJECT.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your mission: Implement Milestone M2 (Algorithmic Core for R3):

1. BACKEND IMPLEMENTATION (Story Arc / Timeline Continuity API):
   - Package `com.khanh.newsaggregator.article.dto`:
     * Create `TimelineEventResponse.java`:
       fields: `Long articleId`, `String title`, `String url`, `String sourceName`, `LocalDateTime publishedAt`, `String phase` (GENESIS, PROGRESSION, LATEST), `String phaseLabel` ("Khởi nguồn", "Diễn biến", "Mới nhất").
     * Create `StoryTimelineResponse.java`:
       fields: `Long articleId`, `String topicTitle`, `List<TimelineEventResponse> events`.
   - Package `com.khanh.newsaggregator.article`:
     * Create `StoryTimelineService.java`:
       Inject `ArticleRepository`, `VietnameseKeywordExtractor`.
       Method `getTimelineForArticle(Long articleId)`:
       Finds article (throw ResourceNotFoundException if absent).
       Extracts entity keywords from title/summary using `VietnameseKeywordExtractor`.
       Finds related articles (same category or sharing keywords within 14 days).
       Sorts chronologically (`publishedAt ASC`).
       Assigns phases: earliest = GENESIS ("Khởi nguồn"), intermediate = PROGRESSION ("Diễn biến"), latest = LATEST ("Mới nhất").
       Returns `StoryTimelineResponse`.
     * In `ArticleController.java`:
       Add endpoint `GET /api/articles/{id}/timeline` returning `ApiResponse<StoryTimelineResponse>`.
       Annotate with `@Operation(summary = "Lấy dòng thời gian sự kiện (Story Arc / Timeline Continuity)")`.
   - Unit Test:
     * Add `StoryTimelineServiceTest.java` in `src/test/java/com/khanh/newsaggregator/article/` testing timeline retrieval and phase assignment.
   - Run verification command:
     `.\mvnw.cmd test-compile` - MUST achieve BUILD SUCCESS (0 errors).

2. FRONTEND ALGORITHMIC CORE in `src/main/resources/static/index.html`:
   - Fix Silent Data Binding Bug: Normalize article objects upon receipt:
     `a.sourceName = a.source?.name || a.sourceName || 'Báo điện tử';`
     `a.categorySlug = a.category?.slug || a.categorySlug || 'thoi-su';`
     `a.categoryName = a.category?.name || a.categoryName || 'Tin tức';`
   - Implement `ThompsonSamplingBandit`:
     Beta-Bernoulli distributions per category with prior $\alpha_k=2, \beta_k=2$ persisted in `localStorage`.
     Function `sampleArm(categories)`: samples $\theta_k \sim \text{Beta}(\alpha_k, \beta_k)$ using Marsaglia-Tsang Gamma method.
     Function `recordFeedback(categorySlug, isPositive)`: updates $\alpha_k$ on engagement, $\beta_k$ on fast skip.
   - Implement `calculateSerendipityScore(article)`:
     $\text{Novelty} \times \text{Relevance} \times \text{Quality}$.
   - Implement dynamic slotting:
     70% Thompson Sampling exploitation, 20% Serendipity Horizon (with badge `✨ Khám phá`), 10% Breaking Urgency.
   - Implement API call `fetchArticleTimeline(articleId)` targeting `/api/articles/{id}/timeline`.

3. Document all implementation details in:
   `e:\News\News-Aggregator-Tracker\.agents\teamwork\worker_m2\handoff.md`.
4. Run `.\mvnw.cmd test-compile` to verify 0 errors.
5. Notify orchestrator via send_message when complete.
