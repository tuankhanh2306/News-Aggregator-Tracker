# Handoff Report — Explorer 3 (UX Behavioral Psychology, Cognitive Load & Discovery Architecture)

**Task:** Survey & Deep Audit for VNNews Hub (R1 Deep Audit Report & R3 Discovery/Story Continuity Architecture)  
**Agent:** Explorer 3 (`explorer_survey_3`)  
**Parent Orchestrator:** `96d014b9-5266-4264-8ff0-492f9a046f15`  
**Date:** 2026-10-03T15:05:00Z  
**Status:** Complete (Hard Handoff)

---

## 1. Observation

### Codebase Observations
1. **Frontend Architecture in `src/main/resources/static/index.html`:**
   - **Reels Deck Structure (Lines 597–641, 1282–1417):**
     - Container is locked to `max-w-[460px] max-h-[820px]` inside `#reelsViewSection`.
     - Snap-scroll configured via `.tiktok-feed` (`scroll-snap-type: y mandatory`, `scroll-snap-stop: always`, lines 57–66).
     - Card background combines `<img>` with `filter brightness-[0.72]` and a dark gradient overlay `bg-gradient-to-t from-black via-black/60 to-black/35` (lines 1314–1319).
     - Text readability relies on a `.summary-box` (`background-color: rgba(10, 11, 14, 0.85); backdrop-filter: blur(14px);` lines 115–121) with `line-clamp-3`.
     - Linear reading timer is driven by `setInterval` every 100ms in `setupImplicitSignalsObserver` (lines 1485–1490), advancing `#readProgress-${articleId}` towards $100\%$ regardless of actual reading comprehension.
     - Action buttons (Avatar, Like, Share, Link) sit in a vertical stack on the right edge (lines 1377–1412), while the Primary CTA "Đọc bản đầy đủ trên báo" is situated on the bottom left (lines 1366–1373).
   - **Bento Grid Structure (Lines 643–663, 1725–1841):**
     - Articles are grouped by date (`Hôm nay`, `Hôm qua`, `Ngày D/M/Y`) into a uniform 3-column CSS grid: `grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5` (line 1771).
     - Each card rendered by `createBentoGridCard(article)` is identical in geometry: cover image `h-40`, source badge, category/time row, 2-line title, 2-line summary, and footer (lines 1791–1841). There are no asymmetric spans (e.g. `col-span-2 row-span-2`).
   - **Algorithmic Scoring & Deck Slotting (Lines 961–1078):**
     - `scoreArticle` calculates `score += userCategoryScore * 12 + matchesTrending * 25 + freshness + sourceCredibility` (lines 961–994).
     - `slotArticleBatch` implements fixed slots: `slot === 0 || 9` for Breaking, `slot === 3 || 7` for Exploration using `leastFavCat = sortedCategories[0][0]` (lines 1026–1050), and fallback to personalized.
     - There is no Thompson Sampling, no Beta distribution, no multi-armed bandit, no timeline clustering, and no serendipity metric.

2. **Backend Architecture & Entity State:**
   - In `src/main/java/com/khanh/newsaggregator/article/Article.java` (lines 10–53), fields are: `id`, `source`, `category`, `title`, `summary`, `url`, `urlHash`, `imageUrl`, `publishedAt`, `fetchedAt`.
   - In `src/main/resources/db/migration/V1__init_schema.sql` through `V4__create_user_and_subscription.sql`, tables are `source`, `category`, `article`, `keyword_stat`, `app_user`, `subscription`.
   - No `story_id`, `cluster_id`, `parent_article_id`, or `timeline` entities exist in any backend file or migration script.
   - `VietnameseKeywordExtractor.java` (lines 10–96) implements named entity extraction via regex (`MULTI_WORD_ENTITY_PATTERN` and `SINGLE_ENTITY_PATTERN`), extracting high-value tokens (e.g., "Hà Nội", "Việt Nam", "SJC", "Apple", "ChatGPT") but is currently only utilized by `TrendingService.java` for 24-hour keyword counts.

3. **Build & Test State:**
   - Tool command: `cmd /c "mvnw.cmd test-compile"` executed cleanly with code 0 (`BUILD SUCCESS`, total time 1.224s).

---

## 2. Logic Chain

1. **Premise 1 (Reading Friction & Cognitive Load):**
   - Directly observed: Text sits over dynamic imagery in Reels; timer bar counts up automatically; and Grid presents 24 identical card shapes simultaneously.
   - *Reasoning:* Dynamic backgrounds cause legibility failure; automatic timer creates time anxiety (timer stress) instead of comprehension; uniform grid cards trigger "snow-blindness" and violate Hick-Hyman Law ($T = b \log_2(n+1)$), overwhelming reader choice.
   - *Inference:* Redesigning Reels with high-contrast double-bezel cards and restructuring Grid into an asymmetric Bento layout (2x2 Lead, 1x2 Spotlight, 1x1 Quick Bites) directly eliminates this cognitive friction.

2. **Premise 2 (Isolated Terminal Nodes & Lost Retention):**
   - Directly observed: Every article in the DB and UI is an isolated row with no links to past or future developments of the same story.
   - *Reasoning:* News develops as sagas over days. Treating articles as terminal endpoints triggers cognitive closure too early and ignores the Zeigarnik Effect (uncompleted tasks/narratives produce memory retention and curiosity).
   - *Inference:* Introducing a Story Arc / Timeline Continuity model—clustering articles via entity overlap from `VietnameseKeywordExtractor` and temporal proximity—allows building a chronological "Mạch Diễn Biến Sự Kiện" stepper/drawer. This triggers the Zeigarnik effect and keeps readers engaged across multi-day stories.

3. **Premise 3 (Filter Bubble & Flawed Exploration):**
   - Directly observed: Current exploration in `index.html:1027` selects `leastFavCat = sortedCategories[0][0]` and injects articles from that category into fixed slots 3 and 7.
   - *Reasoning:* The category with the lowest score is frequently a category the user genuinely dislikes or avoids. Forcing it into fixed slots results in high skip rates and user frustration. It completely lacks probabilistic sampling or reward feedback.
   - *Inference:* Implementing a formal Multi-Armed Bandit using Thompson Sampling with Beta($\alpha_k, \beta_k$) priors enables natural Bayesian exploration with implicit signal feedback (deep dwell $\to \alpha+1$, fast skip $\to \beta+1$). Combining this with a Serendipity score (Novelty $\times$ Relevance $\times$ Quality) bursts the filter bubble without serving unwanted noise.

---

## 3. Caveats

- **External Embedding Models:** We did not assume the presence of heavy vector embedding databases (like Milvus or pgvector) or OpenAI embedding APIs, as the project is a lightweight, self-contained Spring Boot application running on standard PostgreSQL.
- **Client vs Server Coordination:** The Story Arc and Thompson Sampling algorithms can either be executed client-side inside `index.html` (zero server latency, session privacy preserved in `localStorage`) or supported by a lightweight backend `StoryArcService`/`TrendingService`. The proposed architecture supports both seamlessly.
- **Scope Limit:** Explorer 3 is strictly read-only and analytical; no source code files outside of `.agents/teamwork/explorer_survey_3` were modified.

---

## 4. Conclusion

1. **UX Bottlenecks Identified:** VNNews Hub suffers from 4 major cognitive bottlenecks: text legibility interference over image backgrounds, countdown timer anxiety, Hick-Hyman decision fatigue in uniform grid cards, and the isolated atom syndrome of news stories.
2. **5 Breakthrough Psychological Levers Formulated:**
   - *Zeigarnik Narrative Loops:* Story Arc Stepper & "Mạch Diễn Biến Sự Kiện" teaser drawer.
   - *Progressive Disclosure & Curiosity Gap:* 3-Tier card structure (Hook $\to$ 3 Key Takeaways $\to$ Deep Context sheet).
   - *Asymmetric Editorial Bento Grid:* Visual pacing with 2x2 Anchor Lead, 1x2 Vertical Spotlight, and Timeline Strip.
   - *Thumb-Zone Ergonomics & Peak-End Rule:* Bottom action dock, kinetic double-tap to like with particle burst, and gentle Achievement Horizon glow upon read completion.
   - *Serendipity Horizon via Thompson Sampling MAB:* Beta-Binomial bandit per category arm with entity bridge serendipity scoring.
3. **Execution Readiness:** All mathematical formulations, data structures, and UI layout patterns are fully documented in `e:\News\News-Aggregator-Tracker\.agents\teamwork\explorer_survey_3\report.md` for immediate implementation in Phases 2 and 3.

---

## 5. Verification Method

1. **Inspect Survey Report Artifact:**
   - View file: `e:\News\News-Aggregator-Tracker\.agents\teamwork\explorer_survey_3\report.md`
   - Verify that all 5 behavioral psychology opportunities, Story Arc models, and Thompson Sampling math equations are thoroughly defined.
2. **Verify Codebase Alignment:**
   - View `src/main/resources/static/index.html` lines 961–1078 to verify current baseline scoring and slotting logic.
   - View `src/main/java/com/khanh/newsaggregator/trending/VietnameseKeywordExtractor.java` lines 32–96 to verify available entity extraction regex.
3. **Verify Project Compilation Baseline:**
   - Run command: `cmd /c "mvnw.cmd test-compile"` in `e:\News\News-Aggregator-Tracker`
   - Expected result: `BUILD SUCCESS` with 0 compilation errors.
