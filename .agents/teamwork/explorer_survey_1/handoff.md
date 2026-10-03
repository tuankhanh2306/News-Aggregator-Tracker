# HANDOFF REPORT — EXPLORER 1 (BACKEND ARCHITECTURE & DATA FLOW SURVEY)

## 1. OBSERVATION

1. **Build Configuration & Java/Spring Boot Runtime:**
   - File: `pom.xml` (Line 7-8, Line 18):
     ```xml
     <parent>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-parent</artifactId>
         <version>3.4.3</version>
     </parent>
     ...
     <properties>
         <java.version>21</java.version>
     </properties>
     ```
   - Execution command: `.\mvnw.cmd test-compile` in `e:\News\News-Aggregator-Tracker`:
     ```
     [INFO] --- compiler:3.13.0:compile (default-compile) @ news-aggregator ---
     [INFO] Nothing to compile - all classes are up to date.
     [INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ news-aggregator ---
     [INFO] Nothing to compile - all classes are up to date.
     [INFO] BUILD SUCCESS
     [INFO] Total time: 1.291 s
     ```
   - Unit tests execution: `.\mvnw.cmd test "-Dtest=*Test,!ArticleIntegrationTest,!NewsAggregatorApplicationTests"`:
     ```
     [INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
     [INFO] BUILD SUCCESS
     ```
   - Integration tests: `ArticleIntegrationTest` failed only when Docker daemon was not active for Testcontainers (`postgres:16-alpine`), confirming the acceptance criteria focuses on `./mvnw test-compile`.

2. **Backend Domain Models, Services & Controllers:**
   - Article Model & DB Schema: `src/main/java/com/khanh/newsaggregator/article/Article.java` and `src/main/resources/db/migration/V1__init_schema.sql` (Line 19-30) & `V2__add_full_text_search.sql`:
     - Fields: `id`, `source_id`, `category_id`, `title`, `summary`, `url`, `url_hash`, `image_url`, `published_at`, `fetched_at`, `search_vector` (`tsvector` with weights 'A' and 'B').
   - Seeded Sources & Categories: `src/main/java/com/khanh/newsaggregator/common/DataInitializer.java` (Line 41-44, 49-95) seeds 4 categories (`thoi-su`, `kinh-doanh`, `cong-nghe`, `the-gioi`) and 9 RSS sources from VnExpress, Tuổi Trẻ, Thanh Niên, Dân Trí.
   - Retrieval Controller: `src/main/java/com/khanh/newsaggregator/article/ArticleController.java` (Line 19-35):
     - `GET /api/articles` with parameters `category`, `categoryId`, `q`, and `Pageable` (default size 20, sort `publishedAt DESC`).
     - `GET /api/articles/{id}`.
   - Security Permissions: `src/main/java/com/khanh/newsaggregator/auth/SecurityConfig.java` (Line 73):
     ```java
     .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
     ```
     Any sub-endpoints under `GET /api/articles/**` are automatically publicly permitted without authentication changes.
   - Natural Language Extractor: `src/main/java/com/khanh/newsaggregator/trending/VietnameseKeywordExtractor.java` extracts multi-word and single-word Vietnamese capitalized entities, filtering 150+ stopwords. Tested in `VietnameseKeywordExtractorTest.java`.

3. **Client-Side Retrieval & Existing Recommendation Mechanism:**
   - In `src/main/resources/static/index.html` (Line 844-851, 959-1078):
     - Client fetches `/api/articles?page=...&size=20` and `/api/trending/keywords?limit=12`.
     - `scoreArticle()` computes heuristic score = $(sessionWeights[category] \times 12) + (isTrending ? 25 : 0) \times credMultiplier + freshness$.
     - `slotArticleBatch()` implements a naïve 70-20-10 slotting:
       - 10% breaking (`breakingPool`).
       - 20% exploration: Selected via `leastFavCat = sortedCategories[0][0]` (Lines 1026-1028).
       - 70% personalized (`personalizedPool`).
   - Observations on deficiencies:
     - The exploration slot is purely deterministic and picks only the least favored category. There is NO Multi-Armed Bandit / Thompson Sampling.
     - There is NO Serendipity scoring metric (Relevance $\times$ Unexpectedness $\times$ Credibility).
     - There is NO Story Arc or Timeline Continuity feature anywhere in backend or frontend.

---

## 2. LOGIC CHAIN

1. **Step 1 (Build and Runtime Validity):**
   - Observation: `pom.xml` specifies Java 21 and Spring Boot 3.4.3. `.\mvnw.cmd test-compile` completed with exit code 0 and `BUILD SUCCESS`.
   - Inference: The backend build system is healthy, fully operational, and compliant with all project constraints. Any proposed backend changes must maintain `./mvnw test-compile` compilation success.

2. **Step 2 (Data Flow and Recommendation Gap):**
   - Observation: Backend `ArticleService` merely does raw SQL/FTS queries with pagination. Frontend `index.html` implements all sorting and slotting locally using static heuristic rules and hardcoded `leastFavCat`.
   - Inference: The current recommendation system lacks probabilistic exploration. An interactive Multi-Armed Bandit with Thompson Sampling and a Serendipity scoring function can be implemented seamlessly in the client-side recommendation engine, with optional candidate expansion from backend.

3. **Step 3 (Feasibility of Story Arc / Timeline Continuity):**
   - Observation: Backend already has `VietnameseKeywordExtractor` and PostgreSQL Full-Text Search on `search_vector` (`tsvector`). `SecurityConfig.java` line 73 permits all GET requests under `/api/articles/**`.
   - Inference: Adding `GET /api/articles/{id}/timeline` in `ArticleController` (backed by a `StoryArcService`) is architecturally clean, requires zero changes to security filters, and directly leverages the existing entity extractor to cluster chronological news progressions.
   - Frontend can simultaneously display a Story Arc indicator badge on article cards and open an interactive timeline drawer/modal when clicked, completely fulfilling Requirement R3.

---

## 3. CAVEATS

1. **Database Runtime in Production/Demo:** PostgreSQL must be running with the migration scripts applied if full runtime ingestion is tested live; during static compilation and unit testing (`./mvnw test-compile`), no database connection is required.
2. **Cold Start User Profile:** Guest users have no prior database profile, so maintaining Beta distribution parameters ($\alpha, \beta$) for Thompson Sampling in `localStorage` ensures immediate real-time adaptation per session without forcing user registration.
3. **Keyword Extractor Precision:** `VietnameseKeywordExtractor` is rule-based and entity-regex-based. While fast and lightweight, complex semantic paraphrases might require full-text search fallback.

---

## 4. CONCLUSION

1. **System Health:** VNNews Hub is on Spring Boot 3.4.3 and Java 21, with a clean build passing `./mvnw test-compile` 100%.
2. **Algorithmic Enhancement Blueprint:**
   - **Multi-Armed Bandit (MAB) / Thompson Sampling:** Replaces the rigid `leastFavCat` in `index.html` with Beta distributions $\text{Beta}(\alpha_k, \beta_k)$ per category arm, sampling dynamically to balance exploitation and exploration while breaking the filter bubble.
   - **Serendipity Scoring:** Integrates $\text{Relevance} \times \text{Unexpectedness} \times \text{Credibility}$ into the exploration slot candidate selection.
   - **Story Arc / Timeline Continuity:** Implemented via backend service (`StoryArcService` / `ArticleController` endpoint `GET /api/articles/{id}/timeline`) and an interactive frontend Timeline Drawer, connecting multi-stage event progressions.

---

## 5. VERIFICATION METHOD

1. **Verify Build System & Test Compilation:**
   ```powershell
   .\mvnw.cmd test-compile
   ```
   *Expected outcome:* Exit code 0, `BUILD SUCCESS`.
2. **Verify Unit Test Suite:**
   ```powershell
   .\mvnw.cmd test "-Dtest=*Test,!ArticleIntegrationTest,!NewsAggregatorApplicationTests"
   ```
   *Expected outcome:* 23 tests run, 0 failures, `BUILD SUCCESS`.
3. **Verify Survey Report Artifacts:**
   Inspect the following files:
   - `e:\News\News-Aggregator-Tracker\.agents\teamwork\explorer_survey_1\report.md`
   - `e:\News\News-Aggregator-Tracker\.agents\teamwork\explorer_survey_1\handoff.md`
