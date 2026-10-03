# Project: VNNews Hub UX & Discovery Enhancement

## Architecture
- **Backend**: Spring Boot 3.4.3, Java 21, PostgreSQL 16 (Full-Text Search with `tsvector` and GIN index), Redis, Maven Wrapper (`mvnw`).
  - Core Packages:
    - `com.khanh.newsaggregator.article`: REST APIs, DTOs (`ArticleResponse`, `SourceResponse`, `CategoryResponse`), `ArticleService`, `ArticleRepository`.
    - `com.khanh.newsaggregator.article.dto`: `StoryTimelineResponse.java`, `TimelineEventResponse.java`.
    - `com.khanh.newsaggregator.article`: `StoryTimelineService.java`.
    - `com.khanh.newsaggregator.trending`: `TrendingService`, `VietnameseKeywordExtractor`, `KeywordStat`.
- **Frontend**: Single-page editorial news experience in `src/main/resources/static/index.html`.
  - Views:
    1. One-card-per-screen Micro-reading Flow (Reels feed).
    2. Asymmetric Editorial Bento Grid (Date-grouped and Topic-focused).
    3. Story Arc / Narrative Timeline Drawer.
  - Algorithms:
    - Multi-Armed Bandit with Thompson Sampling (Beta-Bernoulli distributions per category).
    - Serendipity Scoring ($\text{Novelty} \times \text{Bridge Relevance} \times \text{Source Credibility}$).
    - Slotting Coordinator ($70\%$ exploitation, $20\%$ serendipity, $10\%$ breaking news).
  - Styling & Design:
    - Tokenized CSS Custom Properties conforming to WCAG AAA contrast ratios ($\ge 7.0:1$ text, $\ge 3.0:1$ UI boundaries) in both Light Mode and Dark Mode.
    - Zero AI-slop; high-end editorial journalistic typography and layout pacing.
    - GPU-accelerated micro-interactions and touch/swipe engine (<100ms latency).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Deep UX Audit Report | Comprehensive audit of One-card and Bento Grid, identifying 5 breakthrough improvements with behavioral psychology & cognitive load | M1 | ORIGINAL_REQUEST §R1 |
| 2 | Silent Data Binding Fix | Fix `article.source?.name`, `article.category?.slug`, `article.category?.name` binding | M2 | Survey Obs 1.2 |
| 3 | Story Arc / Timeline API | Backend endpoint `GET /api/articles/{id}/timeline` linking related narrative articles by entity co-occurrence & chronology | M2 | ORIGINAL_REQUEST §R3 |
| 4 | Thompson Sampling MAB Engine | Multi-Armed Bandit utilizing Beta priors to balance exploitation and exploration, breaking filter bubbles | M2 | ORIGINAL_REQUEST §R3 |
| 5 | Serendipity Discovery Metric | Algorithm calculating serendipity score to inject unexpected yet relevant news items | M2 | ORIGINAL_REQUEST §R3 |
| 6 | WCAG AAA High Contrast Design | Dynamic tokenized CSS variables with verified $\ge 7:1$ contrast in both Light & Dark modes | M3 | ORIGINAL_REQUEST §R2 |
| 7 | Asymmetric Editorial Bento Grid | Replacement of uniform 3-col grid with authentic Bento layout (2x2 Lead, 1x2 Spotlight, 1x1 Quick Bites, Storyline Strip) | M3 | ORIGINAL_REQUEST §R2 |
| 8 | Fluid Gesture & Scroll Engine | Passive event listeners, GPU transforms, eliminate layout reflows, <100ms response | M3 | ORIGINAL_REQUEST §R2 |
| 9 | Tactile Micro-Interactions | Achievement reading glow, kinetic double-tap to like with particle burst, Story Arc drawer | M3 | ORIGINAL_REQUEST §R2 |
| 10 | Build & Integrity Verification | Clean `./mvnw test-compile` (0 errors), challenger validation, forensic audit CLEAN | M4 | ORIGINAL_REQUEST Acceptance |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Deep UX Audit Documentation | Author `DEEP_UX_AUDIT_REPORT.md` with 5 breakthrough behavioral psychology opportunities | Survey completed | DONE |
| M2 | Discovery & Timeline Algorithms | Backend Timeline API & DTOs + Frontend Thompson Sampling MAB & Serendipity Engine | M1 | DONE |
| M3 | Editorial UI/UX Refactor & Micro-interactions | Refactor `src/main/resources/static/index.html` (WCAG AAA, Bento Grid, gestures, micro-interactions) | M2 | DONE |
| M4 | Comprehensive Verification & Delivery | Run `./mvnw test-compile`, Challenger stress tests, Reviewers, Forensic Audit | M3 | IN_PROGRESS |

## Interface Contracts
### Backend Timeline API Contract
- `GET /api/articles/{id}/timeline`
- Response:
  ```json
  {
    "articleId": 123,
    "topicTitle": "Biến động thị trường vàng miếng SJC",
    "timelineEvents": [
      {
        "articleId": 110,
        "title": "Giá vàng miếng chạm đỉnh 91 triệu",
        "publishedAt": "2026-10-01T08:30:00Z",
        "phase": "GENESIS",
        "phaseLabel": "Khởi nguồn"
      },
      {
        "articleId": 120,
        "title": "Ngân hàng Nhà nước thông báo can thiệp thị trường",
        "publishedAt": "2026-10-02T14:00:00Z",
        "phase": "PROGRESSION",
        "phaseLabel": "Diễn biến"
      },
      {
        "articleId": 123,
        "title": "Giá vàng hạ nhiệt về vùng 87 triệu đồng",
        "publishedAt": "2026-10-03T09:00:00Z",
        "phase": "LATEST",
        "phaseLabel": "Mới nhất"
      }
    ]
  }
  ```

### Client MAB & Discovery Contract
- `ThompsonSamplingBandit`:
  - `sampleArm()`: Draw $\theta_k \sim \text{Beta}(\alpha_k, \beta_k)$ for all categories; return winner.
  - `recordFeedback(category, isPositive)`: update $\alpha_k$ or $\beta_k$.
- `SerendipityScorer`:
  - `calculateSerendipityScore(article, userProfile)`: returns score $\in [0, 100]$.
- `DeckSlotter`:
  - 70% Personalized (Thompson Sampling exploited), 20% Serendipity Horizon, 10% Breaking Urgency.

## Code Layout
- Backend Source: `src/main/java/com/khanh/newsaggregator/`
  - `article/`: `ArticleController.java`, `ArticleService.java`, `StoryTimelineService.java`
  - `article/dto/`: `StoryTimelineResponse.java`, `TimelineEventResponse.java`
- Backend Tests: `src/test/java/com/khanh/newsaggregator/article/`
  - `StoryTimelineServiceTest.java`, `ArticleControllerTest.java`
- Frontend Source: `src/main/resources/static/index.html`
- Reports & Docs:
  - `DEEP_UX_AUDIT_REPORT.md` (root and `.agents/teamwork/`)
  - `.agents/teamwork/` (agent metadata, handoffs, reviews, audit verdicts)
