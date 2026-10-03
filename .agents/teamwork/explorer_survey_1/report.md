# BÁO CÁO KHẢO SÁT TOÀN DIỆN KIẾN TRÚC BACKEND & DỮ LIỆU THUẬT TOÁN (VNNEWS HUB)

**Ngày khảo sát:** 2026-10-03  
**Người thực hiện:** Explorer 1 (teamwork_preview_explorer)  
**Mã dự án:** `News-Aggregator-Tracker` (VNNews Hub)  
**Mục tiêu:** Khảo sát chi tiết kiến trúc backend, build system, luồng dữ liệu thuật toán và xây dựng thiết kế khả thi cho R3 (Serendipity & Multi-Armed Bandit / Thompson Sampling, Story Arc / Timeline Continuity).

---

## 1. TỔNG QUAN HỆ THỐNG & CÔNG NGHỆ (BUILD SYSTEM & DEPENDENCIES)

### 1.1. Công nghệ nền tảng & Phiên bản
- **Java Version:** OpenJDK 21 (xác nhận `java.version: 21` trong `pom.xml`, runtime xác nhận `Java 21.0.8`).
- **Framework:** Spring Boot `3.4.3` (`spring-boot-starter-parent:3.4.3`).
- **Build Tool:** Apache Maven 3.x đi kèm Maven Wrapper (`mvnw`, `mvnw.cmd`).
- **Database Engine:** PostgreSQL 16 (hỗ trợ kiểu dữ liệu `tsvector`, chỉ mục GIN cho Full-Text Search).
- **Migration Tool:** Flyway (`flyway-core`, `flyway-database-postgresql`).
- **Cache & In-Memory Store:** Redis (`spring-boot-starter-data-redis` với Jackson2 JSON serializer).
- **RSS Engine:** ROME Tools `2.1.0` (`com.rometools:rome`).
- **Bảo mật:** Spring Security 6.x + JJWT `0.12.6` (`io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`), Stateless JWT.
- **Tài liệu API:** SpringDoc OpenAPI `2.8.5` (`springdoc-openapi-starter-webmvc-ui`).

### 1.2. Đánh giá trạng thái Biên dịch & Kiểm thử (Build & Tests)
- **Lệnh `./mvnw.cmd test-compile`:** Chạy thành công 100% trong ~1.3 giây (`BUILD SUCCESS`, exit code 0). Toàn bộ mã nguồn chính và test code đều tương thích kiểu dữ liệu hoàn hảo với Java 21 và Spring Boot 3.4.3.
- **Unit & Slice Tests:** Chạy 23 unit tests độc lập (`ArticleControllerTest`, `AuthControllerTest`, `JwtTokenProviderTest`, `HashUtilsTest`, `RssContentParserTest`, `RssIngestionSchedulerTest`, `RssIngestionServiceTest`, `SubscriptionControllerTest`, `TrendingControllerTest`, `VietnameseKeywordExtractorTest`) đạt **23/23 PASSED (100%)**.
- **Lưu ý về Integration Tests:** `ArticleIntegrationTest` và `NewsAggregatorApplicationTests` yêu cầu Docker daemon hoặc PostgreSQL localhost:5432 đang chạy để khởi tạo Testcontainers `postgres:16-alpine`. Do đó tiêu chí nghiệm thu của bài toán tập trung chuẩn xác vào `./mvnw test-compile`.

---

## 2. BẢN ĐỒ KIẾN TRÚC BACKEND & DOMAIN ENTITIES

### 2.1. Lược đồ Cơ sở dữ liệu (Database Schema Migrations)
1. **`source` (V1__init_schema.sql):**
   - Quản lý các kênh RSS: `id` (BIGSERIAL PK), `name` (VARCHAR 100), `rss_url` (VARCHAR 500 UNIQUE), `active` (BOOLEAN), `last_fetched_at` (TIMESTAMP).
   - Được nạp sẵn 9 nguồn chính thống qua `DataInitializer`: VnExpress (Tin mới, Thời sự, Kinh doanh, Khoa học công nghệ, Thế giới), Tuổi Trẻ (Tin mới, Nhịp sống số), Thanh Niên (Trang chủ), Dân Trí (Sự kiện).
2. **`category` (V1__init_schema.sql):**
   - Phân loại bài viết: `id` (BIGSERIAL PK), `name` (VARCHAR 50), `slug` (VARCHAR 50 UNIQUE).
   - Được nạp sẵn 4 danh mục: `thoi-su` (Thời sự), `kinh-doanh` (Kinh doanh), `cong-nghe` (Công nghệ), `the-gioi` (Thế giới).
3. **`article` (V1__init_schema.sql & V2__add_full_text_search.sql):**
   - Bảng dữ liệu tin tức trọng tâm:
     - `id` (BIGSERIAL PK)
     - `source_id` (BIGINT FK `source.id` ON DELETE CASCADE)
     - `category_id` (BIGINT FK `category.id` ON DELETE SET NULL)
     - `title` (VARCHAR 500 NOT NULL)
     - `summary` (TEXT)
     - `url` (VARCHAR 1000 NOT NULL)
     - `url_hash` (CHAR 64 UNIQUE SHA-256) chống trùng lặp tuyệt đối
     - `image_url` (VARCHAR 1000)
     - `published_at` (TIMESTAMP)
     - `fetched_at` (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
     - `search_vector` (`tsvector` tính toán tự động qua trigger PostgreSQL `article_search_vector_update` gán trọng số 'A' cho Title, 'B' cho Summary).
   - Chỉ mục: `idx_article_source_id`, `idx_article_category_id`, `idx_article_published_at DESC`, `idx_article_url_hash`, `idx_article_search_vector` (GIN).
4. **`keyword_stat` (V3__create_keyword_stat.sql):**
   - Thống kê từ khóa thịnh hành: `id` (BIGSERIAL PK), `keyword` (VARCHAR 100), `window_start` (TIMESTAMP), `count` (INT).
   - Chỉ mục: `idx_keyword_stat_keyword_window`, `idx_keyword_stat_window_start DESC`.
5. **`app_user` & `subscription` (V4__create_user_and_subscription.sql):**
   - Quản lý tài khoản độc giả và danh mục đăng ký nhận bản tin digest qua email.

### 2.2. Chi tiết các Controller, Service, Repository

| Package | Component | Vai trò & Hành vi |
|---|---|---|
| `article` | `ArticleController` | Cung cấp REST endpoints: <br>`GET /api/articles` (hỗ trợ `category`, `categoryId`, `q`, `page`, `size`, `sort`)<br>`GET /api/articles/{id}` |
| `article` | `ArticleService` | Điều phối truy vấn bài viết từ DB, quản lý cache Redis (`CACHE_ARTICLES` TTL 5m, `CACHE_ARTICLE_DETAIL` TTL 10m). Xử lý phân nhánh Full-Text Search qua `tsvector` và lọc theo danh mục. |
| `article` | `ArticleRepository` | Kế thừa `JpaRepository`, tối ưu eager fetch quan hệ `source` và `category` bằng `@EntityGraph` triệt tiêu lỗi N+1 queries; cung cấp native queries FTS với `ts_rank`. |
| `trending` | `TrendingController` | `GET /api/trending/keywords?limit=10` |
| `trending` | `TrendingService` | Thu thập bài viết 24h gần nhất (hoặc 500 bài mới nhất), trích xuất thực thể/từ khóa, đếm tần suất và lưu vào `keyword_stat`, cache Redis TTL 15m. |
| `trending` | `VietnameseKeywordExtractor` | Trích xuất thực thể viết hoa (Named Entities) đa từ và đơn từ, loại bỏ hơn 150 stopwords tiếng Việt. |
| `ingestion` | `RssIngestionService` | Thu thập tin tức từ RSS qua ROME XML, băm SHA-256 URL để deduplicate, phân loại danh mục tự động theo từ khóa, lưu trữ hàng loạt vào `article`. |
| `category` | `CategoryController` & `CategoryService` | `GET /api/categories`, cache Redis TTL 60m. |
| `auth` | `SecurityConfig` | Cấu hình bảo mật Spring Security: công khai (`permitAll()`) cho các đường dẫn `GET /api/articles/**`, `GET /api/categories/**`, `GET /api/trending/**`, `/api/auth/**`, `/index.html`. |

---

## 3. ĐÁNH GIÁ CƠ CHẾ GỢI Ý & SẮP XẾP HIỆN TẠI (CURRENT SORTING & RECOMMENDATIONS)

### 3.1. Phía Backend
- Backend hiện đóng vai trò **Data Provider thuần túy**:
  - `ArticleController` chỉ nhận truy vấn và phân trang theo `publishedAt DESC` hoặc độ khớp FTS `ts_rank DESC`.
  - Chưa có bất kỳ logic nhóm cụm sự kiện (Clustering), gợi ý khám phá (Serendipity), hay chuỗi diễn biến tin tức (Story Arc / Timeline) nào ở tầng Service/Repository.

### 3.2. Phía Client (`src/main/resources/static/index.html`)
- Giao diện nạp dữ liệu phân trang từ `/api/articles` và từ khóa từ `/api/trending/keywords`.
- Động cơ xếp hạng hiện tại là **Heuristic cục bộ trong phiên duyệt web**:
  - `scoreArticle(article)`: Tính điểm dựa trên trọng số danh mục người dùng tương tác trong phiên (`sessionWeights[category] * 12`), cộng điểm từ khóa trending (+25), nhân hệ số uy tín nguồn báo chí (`SOURCE_CREDIBILITY`: VnExpress 1.25, Tuổi Trẻ 1.20...), và cộng điểm độ tươi mới (tin dưới 4h: +15, dưới 12h: +8).
  - Thuật toán phân bổ khe đọc (Slotting 70 - 20 - 10):
    - **10% Tin nóng (Breaking):** Lấy từ danh sách khớp từ khóa trending (`isTrendingHot`).
    - **20% Khám phá (Exploration - Hiện tại):** **ĐANG LÀ LỖI THIẾT KẾ NGHIÊM TRỌNG**. Hệ thống hiện chỉ chọn bài viết từ danh mục ít được quan tâm nhất (`leastFavCat = sortedCategories[0][0]`). Đây là phương pháp tĩnh, cứng nhắc và hoàn toàn không phải Multi-Armed Bandit / Thompson Sampling. Nếu người dùng ghét mục Thế giới, hệ thống cứ cố tình ép đọc mục Thế giới ở mỗi chu kỳ!
    - **70% Cá nhân hóa (Personalized):** Sắp xếp theo điểm `_score` giảm dần.
  - Tín hiệu hành vi ngầm định (Implicit Signals):
    - Đo Dwell Time qua `IntersectionObserver`: Lướt nhanh < 1.5s trừ 10 điểm; Đọc kỹ > 65% thời lượng chuẩn cộng 20 điểm; Bấm đọc bài gốc cộng 35 điểm; Thả tim cộng 25 điểm.
- **Khoảng trống cốt lõi:**
  1. Chưa có Multi-Armed Bandit / Thompson Sampling để giải quyết bài toán Đánh đổi Khai thác - Khám phá (Exploration vs Exploitation) một cách ngẫu nhiên có định hướng xác suất Bayesian.
  2. Chưa có công thức đo lường Độ bất ngờ & Khám phá thú vị (Serendipity Metric).
  3. Hoàn toàn không có tính năng **Dòng thời gian sự kiện (Story Arc / Timeline Continuity)**: Các bài viết về cùng một diễn biến lớn bị rời rạc, độc giả không thể xem được mạch câu chuyện phát triển từ đâu và đi về đâu.

---

## 4. THIẾT KẾ CHI TIẾT TRIỂN KHAI YÊU CẦU R3

### 4.1. Thuật toán Multi-Armed Bandit (MAB) với Thompson Sampling
Để phá vỡ bong bóng thông tin (Filter Bubble) mà vẫn giữ được sự thích thú của người đọc, ta thiết kế mô hình **Bernoulli / Beta Thompson Sampling**:

#### 1. Định nghĩa các "Cánh tay gạt" (Arms $K$):
- Mỗi danh mục tin tức $k \in \{\text{Thời sự, Kinh doanh, Công nghệ, Thế giới}\}$ (hoặc cụm chủ đề chuyên sâu) là một cánh tay gạt $k$.
- Với mỗi cánh tay $k$, hệ thống duy trì một phân phối tiền nghiệm Beta:
  $$\theta_k \sim \text{Beta}(\alpha_k, \beta_k)$$
  - $\alpha_k$: Số lần thành công tích lũy (tương tác tích cực: Đọc hết thẻ $\ge 65\%$, Bấm đọc bài gốc, Thả tim, Chia sẻ). Ban đầu khởi tạo $\alpha_k = 1.0$ (Prior trung hòa).
  - $\beta_k$: Số lần thất bại tích lũy (lướt nhanh qua thẻ $< 1.5s$, bỏ qua). Khởi tạo $\beta_k = 1.0$.

#### 2. Cơ chế Lấy mẫu Thompson Sampling (Sampling Step):
- Tại mỗi lượt phân bổ bài viết vào vị trí Khám phá (Exploration Slot - ví dụ vị trí thứ 3 và thứ 7 trong chu kỳ 10 thẻ):
  - Lấy mẫu ngẫu nhiên giá trị kỳ vọng từ phân phối Beta của từng cánh tay:
    $$\hat{\theta}_k \sim \text{Beta}(\alpha_k, \beta_k)$$
  - Cánh tay có giá trị mẫu cao nhất sẽ được chọn để đại diện cho lượt khám phá:
    $$k^* = \arg\max_{k} \hat{\theta}_k$$
- **Cơ sở toán học & Ưu điểm vượt trội:**
  - Đối với các chủ đề người dùng chưa đọc nhiều, phương sai của $\text{Beta}(\alpha_k, \beta_k)$ rất rộng, cho phép cánh tay này có xác suất sinh ra giá trị mẫu lớn đột xuất để được trao cơ hội xuất hiện (Exploration).
  - Đối với các chủ đề người dùng ghét (nhiều lần lướt nhanh), $\beta_k$ lớn khiến phân phối co hẹp về gần 0, hệ thống tự động giảm tần suất xuất hiện mà không cần hardcode.
  - Đạt mức chặn hối tiếc tối ưu lý thuyết $O(\log T)$, giải quyết triệt để sự gò bó của thuật toán cũ.

#### 3. Thuật toán sinh biến ngẫu nhiên Beta trên Client:
Hàm sinh biến ngẫu nhiên Beta từ phân phối Gamma (hoặc biến đổi xấp xỉ chính xác cao) có thể thực hiện siêu nhẹ trên JavaScript:
```javascript
function sampleBeta(alpha, beta) {
    // Marsaglia and Tsang method hoặc biến đổi chuẩn cho Alpha/Beta
    const u = sampleGamma(alpha, 1);
    const v = sampleGamma(beta, 1);
    return u / (u + v);
}
```

---

### 4.2. Thuật toán Khám phá Bất ngờ (Serendipity Scoring)
Một bài viết khám phá không thể là một tin rác ngẫu nhiên. Nó phải thỏa mãn nguyên lý **Serendipity = Sự bất ngờ thú vị**:
$$\text{Serendipity}(a) = \text{Relevance}(a) \times \text{Unexpectedness}(a) \times \text{Credibility}(a)$$

Trong đó:
1. **$\text{Relevance}(a)$ (Chất lượng nội dung & Độ hấp dẫn):**
   - Đánh giá qua độ tươi mới của bài viết và sự chú ý chung từ cộng đồng:
     $$\text{Relevance}(a) = 0.6 \cdot \text{FreshnessFactor}(a) + 0.4 \cdot \text{TrendingBonus}(a)$$
2. **$\text{Unexpectedness}(a)$ (Độ bất ngờ / Độ phân kỳ với sở thích quen thuộc):**
   - Đo lường khoảng cách giữa chủ đề của bài viết $a$ với tâm sở thích lịch sử của độc giả:
     $$\text{Unexpectedness}(a) = 1.0 - \text{CosineSimilarity}(\vec{C}_a, \vec{U}_{\text{profile}})$$
   - Bài viết thuộc chủ đề độc giả ít đọc nhưng có điểm chất lượng cao sẽ nhận điểm bất ngờ tối đa.
3. **$\text{Credibility}(a)$ (Hệ số nguồn uy tín chống Clickbait):**
   - Áp dụng trọng số từ `SOURCE_CREDIBILITY` (VnExpress, Tuổi Trẻ, Thanh Niên...) nhằm đảm bảo bài khám phá luôn có tính chuẩn xác, tránh tin giật gân rẻ tiền.

---

### 4.3. Thuật toán Dòng thời gian sự kiện (Story Arc / Timeline Continuity)

#### 1. Nguyên lý nhận diện Dòng thời gian sự kiện:
- Tin tức báo chí thường phát triển theo chuỗi (Story Arc):
  - *Giai đoạn 1 (Khởi phát):* Biến cố hoặc sự việc đầu tiên diễn ra.
  - *Giai đoạn 2 (Diễn biến):* Cơ quan chức năng vào cuộc, các tình tiết mới được công bố, góc nhìn phân tích.
  - *Giai đoạn 3 (Cập nhật mới nhất):* Kết luận hoặc diễn biến mới nhất hôm nay.
- Hai bài viết $A$ và $B$ thuộc cùng một dòng thời gian khi:
  1. Khoảng cách thời gian xuất bản nằm trong cửa sổ sự kiện (thường trong vòng 3 đến 7 ngày).
  2. Có độ tương đồng thực thể định danh (Named Entity Overlap) vượt ngưỡng:
     $$\text{Jaccard}(E_A, E_B) = \frac{|E_A \cap E_B|}{|E_A \cup E_B|} \ge \theta_{\text{threshold}}$$
     (hoặc chia sẻ ít nhất 2 thực thể đặc trưng như tên riêng, địa danh, mã vụ việc).

#### 2. Thiết kế tầng Backend:
- Đã có sẵn công cụ cực mạnh trong codebase: `VietnameseKeywordExtractor`.
- **Đề xuất bổ sung Endpoint:**
  1. `GET /api/articles/{id}/timeline`:
     - Nhận vào ID bài viết hiện tại.
     - Trích xuất thực thể của bài viết $A$, tìm các bài viết liên quan trong 7 ngày gần nhất qua Full-Text Search hoặc truy vấn bộ lọc thực thể.
     - Tính điểm liên kết sự kiện, sắp xếp theo trình tự thời gian từ cũ đến mới (`publishedAt ASC`).
     - Trả về danh sách diễn biến:
       ```json
       {
         "targetArticleId": 101,
         "storyArcTitle": "Diễn biến sự kiện liên quan",
         "events": [
           { "id": 85, "title": "Khởi đầu...", "stage": "Khởi phát", "publishedAt": "2026-10-01 08:30:00", "source": "Tuổi Trẻ" },
           { "id": 101, "title": "Diễn biến tiếp theo...", "stage": "Diễn biến", "publishedAt": "2026-10-02 14:15:00", "source": "VnExpress" },
           { "id": 118, "title": "Cập nhật mới nhất...", "stage": "Mới nhất", "publishedAt": "2026-10-03 10:00:00", "source": "Thanh Niên" }
         ]
       }
       ```
  2. `GET /api/articles/story-arcs`:
     - Tự động gom cụm các sự kiện nóng nhất 72h qua thành các dòng thời gian nổi bật (Featured Timelines) phục vụ trang chủ Bento Grid.

#### 3. Thiết kế tầng Frontend (UI/UX Micro-Interactions):
- Trên mỗi thẻ tin (cả Reels Card lẫn Bento Grid Card):
  - Hiển thị huy hiệu tinh tế: `[ Dòng thời gian: 3 bài viết liên quan ]` kèm icon timeline phát sáng nhẹ.
  - Khi người dùng click, mở một **Timeline Drawer / Drawer trượt mượt mà** thể hiện các mốc thời gian diễn tiến của câu chuyện theo chiều dọc với các nút chuyển nhanh giữa các bài viết.
  - Tạo động lực tâm lý tò mò: *"Câu chuyện bắt đầu như thế nào?"* và *"Diễn biến mới nhất ra sao?"*, giữ chân độc giả lâu hơn gấp nhiều lần so với việc đọc một tin lẻ tẻ.

---

## 5. ĐÁNH GIÁ TÍNH TƯƠNG THÍCH BẢO MẬT & API (SECURITY & COMPATIBILITY)

1. **Quy tắc bảo mật Spring Security (`SecurityConfig.java`):**
   - Dòng 73: `.requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()`
   - **Lợi thế kiến trúc:** Bất kỳ endpoint GET nào được thêm vào dưới `/api/articles/**` (ví dụ `/api/articles/{id}/timeline`, `/api/articles/story-arcs`) đều **mặc định công khai** mà không làm thay đổi các quy tắc phân quyền người dùng của Spring Security!
2. **Khả năng tương thích ngược (Backward Compatibility):**
   - Cấu trúc DTO `ArticleResponse` hiện tại hoàn toàn nguyên vẹn.
   - Thêm trường `timelineCount` hoặc `storyArcId` vào `ArticleResponse` (hoặc cung cấp endpoint riêng) đều không phá vỡ bất kỳ giao diện cũ nào.
3. **Hiệu năng & Cache:**
   - Sử dụng Redis cache cho kết quả timeline với TTL 10 phút, tránh truy vấn lặp lại nhiều lần trên cùng một sự kiện tin tức.

---

## 6. KẾT LUẬN & ĐỀ XUẤT CHO BƯỚC TRIỂN KHAI

| Hạng mục | Khuyến nghị kỹ thuật | Trách nhiệm thực thi |
|---|---|---|
| **Build System** | Bảo toàn tính toàn vẹn `./mvnw test-compile` (Java 21, Spring Boot 3.4.3). | Toàn đội ngũ |
| **Backend Service** | Xây dựng `StoryArcService` và mở rộng `ArticleController` (`GET /api/articles/{id}/timeline`, `GET /api/articles/story-arcs`) tận dụng `VietnameseKeywordExtractor`. | Backend Team |
| **Frontend UI/UX** | Nâng cấp trực tiếp `src/main/resources/static/index.html`: <br>1. Thay thế khe 20% khám phá tĩnh bằng **Thompson Sampling Beta Distribution** & **Serendipity Scoring**.<br>2. Tích hợp thanh đo đọc, hiệu ứng tim, High Contrast Light/Dark chuẩn WCAG AAA.<br>3. Tích hợp giao diện **Dòng thời gian sự kiện (Story Arc Drawer / Timeline Widget)**. | Frontend & UX Team |
| **Kiểm thử** | Bổ sung unit test cho `StoryArcService` và đảm bảo `./mvnw test-compile` đạt 100% BUILD SUCCESS. | Testing & Quality |

---
*Báo cáo được hoàn thành và đối chiếu thực tế với toàn bộ mã nguồn tại `e:\News\News-Aggregator-Tracker`.*
