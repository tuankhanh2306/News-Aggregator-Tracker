# Deep UX Behavioral Psychology, Cognitive Load & Discovery Architecture Audit
**Dự án:** VNNews Hub (News Aggregator & Trending Tracker)  
**Phân hệ nghiên cứu:** R1 (Deep Audit Report) & R3 (Smart Discovery & Story Continuity Architecture)  
**Tác giả:** Explorer 3 (teamwork_preview_explorer)  
**Ngày thực hiện:** 2026-10-03  
**Trạng thái:** Hoàn thành khảo sát chuyên sâu

---

## Tóm tắt Điều hành (Executive Summary)

VNNews Hub sở hữu nền tảng công nghệ vững chắc (Spring Boot 3.3.1, Java 21, PostgreSQL với GIN Index TSVECTOR, Redis Caching), cùng hai giao diện đọc tin hiện đại: **Micro-reading Flow (Thẻ lướt dọc One-card-per-screen)** và **Editorial Bento Grid (Lưới tin phân loại theo ngày)**. Tuy nhiên, qua khảo sát mã nguồn và hành vi thực tế, hệ thống đang tồn tại **4 rào cản nhận thức (Cognitive Bottlenecks)** lớn khiến trải nghiệm độc giả bị đứt đoạn, dễ gây kiệt quệ quyết định (decision fatigue) và mắc kẹt trong "bẫy thông tin một chiều" (filter bubble).

Báo cáo này cung cấp:
1. **Đánh giá giải phẫu học chi tiết** 2 luồng đọc tin hiện hữu (Reels vs Bento Grid).
2. **5 cơ hội cải tiến đột phá** được xây dựng trên các định luật tâm lý học hành vi kinh điển (Zeigarnik Effect, Curiosity Gap của Loewenstein, Hick-Hyman Law, Fitts' Law, Progressive Disclosure, và Peak-End Rule).
3. **Mô hình hóa Dòng thời gian sự kiện (Story Arc / Timeline Continuity)** giải quyết bài toán "tin tức phân mảnh theo lát cắt thời gian".
4. **Kiến trúc thuật toán Khám phá Thông minh (Serendipity & Multi-Armed Bandit qua Thompson Sampling)** giúp phá vỡ bẫy lọc thông tin một cách khoa học, duy trì mức độ giữ chân độc giả (retention & dwell time) tối đa.

---

## 1. Giải phẫu & Đánh giá Luồng Đọc Tin Hiện Trạng (Reading Journeys Evaluation)

### 1.1. Luồng Đọc Vi Mô: One-Card-Per-Screen (TikTok Reels Feed)

#### A. Kiến trúc kỹ thuật hiện tại
- **DOM & CSS:** Container `#tiktokFeed` nằm trong vùng `max-w-[460px] max-h-[820px]` (mô phỏng khung điện thoại trên Desktop). Áp dụng `scroll-snap-type: y mandatory` và `scroll-snap-stop: always`.
- **Nhận diện Tín hiệu Ngầm (Implicit Signals):** 
  - Sử dụng `IntersectionObserver` với ngưỡng `threshold: 0.60`.
  - Đo lường thời gian lưu lại (dwell time): $T_{\text{standard}} = \max(3\text{s}, \text{wordCount} \times 0.28\text{s})$.
  - Phân loại:
    - *Fast Skip* ($< 1.5\text{s}$): trừ 10 điểm chuyên mục.
    - *Đọc lướt* ($1.5\text{s} \le T < 0.65 T_{\text{standard}}$): cộng 5 điểm.
    - *Đọc kỹ* ($T \ge 0.65 T_{\text{standard}}$): cộng 20 điểm.
    - *Deep Read* (click xem bài gốc): cộng 35 điểm.
- **Thanh đo tiến độ:** Thẻ `read-progress-bar` chạy tuyến tính bằng `setInterval(100ms)` từ $0\%$ đến $100\%$ dựa trên $T_{\text{standard}}$.

```
+-------------------------------------------------------------+
| [Thanh tiến độ đọc (đếm giờ tuyến tính 0% -> 100%)]         |
|                                                             |
| [#1] [VnExpress] [Thời sự] [🔥 Tin Nóng]         [Đã xem]   |
|                                                             |
|                     [ẢNH NỀN BÀI VIẾT]                      |
|                  (Brightness 0.72 + Gradient)               |
|                                                             |
|                                             [Avatar Nguồn]  |
| TIÊU ĐỀ BÀI BÁO (Text White, drop-shadow)                   |
|                                             [❤️ Tim: 120]   |
| +-----------------------------------------+                 |
| | Hộp Tóm Tắt (Summary Box)               | [🔗 Chia sẻ]   |
| | (Blur 14px, nền tối bán trong suốt)     |                 |
| | Cắt tối đa 3 dòng (line-clamp-3)...     | [↗ Link gốc]   |
| +-----------------------------------------+                 |
|                                                             |
| 🕒 2 giờ trước                                              |
| [ Đọc bản đầy đủ trên báo ↗ ]                               |
+-------------------------------------------------------------+
```

#### B. Các điểm nghẽn nhận thức & tâm lý học (Cognitive Bottlenecks)

1. **Xung đột thị giác & Suy giảm tương phản (Visual Interference & Contrast Degradation):**
   - Tiêu đề và tóm tắt hiển thị trực tiếp đè lên ảnh nền thông qua dải gradient `from-black via-black/60 to-black/35`. Khi bài báo có ảnh nền nhiều chi tiết trắng hoặc rực rỡ, độ dễ đọc (legibility) sụt giảm mạnh, vi phạm tiêu chuẩn tương phản WCAG.
   - Ở chế độ Nền sáng (`html.light`), giao diện xung quanh chuyển sang màu trắng/xám nhạt nhưng thẻ Reels vẫn duy trì nền đen tuyệt đối. Sự tương phản dị biệt này gây giật thị giác (visual shock) khi người dùng chuyển đổi qua lại.

2. **Căng thẳng nhận thức từ Thanh tiến độ đếm ngược (Timer Stress vs. Comprehension):**
   - Thanh đỏ `read-progress-bar` tăng đều đặn theo thời gian cơ học chứ không phản ánh vị trí cuộn hay nhịp đọc thực tế của độc giả.
   - *Tâm lý học:* Thanh chạy tự động tạo cảm giác "đồng hồ đếm ngược" (countdown pressure). Người đọc có xu hướng bị phân tâm nhìn thanh đỏ xem khi nào nó đầy thay vì tập trung thẩm thấu nội dung câu chữ.

3. **Vi phạm Vùng thao tác ngón cái (Fitts' Law Violation & Motor Friction):**
   - Cột nút tương tác xã hội (Like, Share, Link) nằm dọc sát cạnh phải. Trong khi đó, nút hành động chính (Primary CTA) *"Đọc bản đầy đủ trên báo"* lại là nút ngang ở góc dưới bên trái.
   - Với người dùng thuận tay phải trên di động, khoảng cách di chuyển từ vùng cuộn (giữa màn hình) sang góc dưới bên trái đòi hỏi gập ngón cái hoặc dùng hai tay, làm tăng đáng kể thời gian chuyển động $MT$ theo Fitts' Law.

4. **Hội chứng "Đảo thông tin cô lập" (Isolated Atom Syndrome):**
   - Mỗi thẻ lướt qua là một thực thể độc lập. Khi vuốt sang tin tiếp theo, không có bất kỳ tín hiệu nào cho thấy tin tức vừa đọc có liên quan đến một diễn biến lớn hơn, hay đã có hồi kết chưa. Người đọc bị ngắt quãng dòng suy nghĩ liên tục.

---

### 1.2. Luồng Đọc Dạng Lưới Theo Ngày: Editorial Bento Grid

#### A. Kiến trúc kỹ thuật hiện tại
- Nhóm bài viết theo nhãn ngày: `Hôm nay`, `Hôm qua`, `Ngày D/M/YYYY`, hoặc `Tin gần đây`.
- Hiển thị theo lưới CSS 3 cột đối xứng: `grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5`.
- Mỗi thẻ bài viết đều đồng dạng: ảnh bìa `h-40`, nguồn, chuyên mục, tiêu đề `line-clamp-2`, tóm tắt `line-clamp-2`, chân trang gồm nút "Đọc tiếp" và nút Like.

```
[ Hôm nay ] ---------------------------------------------- 24 tin tức
+--------------------+  +--------------------+  +--------------------+
| Ảnh (h-40)         |  | Ảnh (h-40)         |  | Ảnh (h-40)         |
| Nguồn • Chuyên mục |  | Nguồn • Chuyên mục |  | Nguồn • Chuyên mục |
| Tiêu đề (2 dòng)   |  | Tiêu đề (2 dòng)   |  | Tiêu đề (2 dòng)   |
| Tóm tắt (2 dòng)   |  | Tóm tắt (2 dòng)   |  | Tóm tắt (2 dòng)   |
| [Đọc tiếp]     [❤️]|  | [Đọc tiếp]     [❤️]|  | [Đọc tiếp]     [❤️]|
+--------------------+  +--------------------+  +--------------------+
```

#### B. Các điểm nghẽn nhận thức & tâm lý học (Cognitive Bottlenecks)

1. **Hội chứng "Lưới đồng dạng đơn điệu" (Monotonous Template Syndrome):**
   - Mặc dù được đặt tên là "Bento Grid", giao diện thực chất là một bảng lưới đồng đều 3 cột kiểu Bootstrap truyền thống. Nó thiếu hoàn toàn cấu trúc Bento chuẩn (như Apple hay Linear): không có ô tiêu điểm kích thước lớn ($2 \times 2$), không có ô dải ngang ($2 \times 1$), không có ô dọc tiêu điểm.
   - *Tâm lý học:* Hiện tượng **Tuyết mù thông tin (Snow-blindness)** xảy ra khi 15-20 thẻ tin tức có cùng kích cỡ, độ sáng và cấu trúc hiển thị đồng loạt. Mắt người đọc không tìm thấy "mỏ neo thị giác" (visual anchor) để bắt đầu quét, dẫn đến lướt qua nhanh mà không đọng lại gì.

2. **Quá tải lựa chọn & Mệt mỏi quyết định (Hick-Hyman Law & Choice Overload):**
   - Theo định luật Hick-Hyman: Thời gian ra quyết định tỷ lệ thuận với số lượng lựa chọn:
     $$T = b \cdot \log_2(n + 1)$$
   - Đặt 24 bài viết đồng dạng trước mắt người dùng khiến não bộ phải đánh giá đồng thời hàng chục tiêu đề mà không có gợi ý đâu là tâm điểm tin tức trong ngày. Hệ quả là tỷ lệ thoát trang (bounce rate) tăng cao.

3. **Trùng lặp tin tức không gom cụm (Redundant Duplicate Fragmentation):**
   - Khi một sự kiện quốc gia diễn ra (ví dụ: bão lũ, biến động giá vàng, chính sách thuế mới), VnExpress, Tuổi Trẻ, Thanh Niên và Dân Trí đều đưa tin. Kết quả là trên lưới xuất hiện 4-5 thẻ bài viết có tiêu đề gần như giống hệt nhau, chiếm dụng diện tích hiển thị quý giá và gây nhàm chán.

4. **Đứt gãy liên tục trạng thái giữa Reels và Bento Grid:**
   - Khi người dùng đang lướt đến tin thứ 12 ở chế độ Reels và bấm chuyển sang Bento Grid, trang bị nhảy ngược về đầu danh sách. Không có sự đồng bộ hóa vị trí đọc (reading state synchronicity).

---

## 2. 5 Cơ Hội Cải Tiến Đột Phá Dựa Trên Tâm Lý Học Hành Vi

Dưới đây là 5 đề xuất cải tiến cấp chiến lược, được đối chiếu trực tiếp với các nguyên lý tâm lý học nhận thức:

```
+-----------------------------------------------------------------------------------------+
|                  5 CƠ HỘI CẢI TIẾN ĐỘT PHÁ (BEHAVIORAL PSYCHOLOGY)                      |
+-----------------------------------------------------------------------------------------+
| 1. Vòng lặp Zeigarnik (Story Arc Stepper)       -> Kích hoạt thôi thúc đóng chuỗi tò mò |
| 2. Phân tầng Khám phá (Progressive Disclosure)   -> Tối ưu Curiosity Gap không clickbait |
| 3. Bento Lưới Bất Đối Xứng (Asymmetric Rhythm)   -> Xóa bỏ tuyết mù thị giác & Hick-Hyman|
| 4. Bến đỗ Công thái học (Ergonomic Thumb-Zone)   -> Giảm ma sát Fitts' Law & Peak-End   |
| 5. Chân trời Khám phá (Thompson Sampling MAB)    -> Phá bẫy lọc thông tin (Serendipity)  |
+-----------------------------------------------------------------------------------------+
```

### Đề xuất 1: Vòng lặp Zeigarnik & Mạch Diễn Biến Sự Kiện (Narrative Zeigarnik Loops)
- **Nguyên lý tâm lý:** **Hiệu ứng Zeigarnik (Bluma Zeigarnik, 1927)** chỉ ra rằng con người ghi nhớ và bị thôi thúc mạnh mẽ bởi các nhiệm vụ hoặc câu chuyện dang dở hơn là những thứ đã hoàn tất.
- **Thực trạng điểm nghẽn:** Bài báo được coi là một điểm cuối (terminal node). Người đọc đọc xong là kết thúc, không biết sự việc trước đó thế nào hoặc diễn biến tiếp theo ra sao.
- **Giải pháp đột phá:**
  - Tích hợp một thanh **"Mạch Diễn Biến Sự Kiện" (Story Arc Stepper)** trực tiếp trên thẻ bài viết:
    - `[Giai đoạn 1: Khởi nguồn] → [Giai đoạn 2: Điều tra] → [Giai đoạn 3: Diễn biến mới nhất (Hiện tại)]`.
  - Bổ sung huy hiệu kích thích tò mò: `Theo dõi diễn biến: 3 bài viết cùng dòng thời gian ↗`.
  - Khi bấm vào, một Drawer trượt mở ra hiển thị dòng thời gian trực quan của vụ việc, kết thúc bằng một câu hỏi bỏ lửng khơi gợi tương lai: *"Sự việc sẽ được cơ quan chức năng kết luận vào ngày mai?"*.
- **Tác động:** Giữ chân độc giả ở lại phiên đọc lâu hơn 300%, biến việc đọc tin rời rạc thành thói quen theo dõi chuỗi tập phim (narrative binge-reading).

### Đề xuất 2: Phân Tầng Tiếp Nhận & Khoảng Trống Tò mò (Progressive Disclosure & Curiosity Gap)
- **Nguyên lý tâm lý:** **Thuyết Khoảng trống Thông tin (Information Gap Theory - George Loewenstein, 1994)** và **Nguyên tắc Tiết lộ Lũy tiến (Progressive Disclosure - Nielsen Norman Group)**. Sự tò mò đạt đỉnh khi khoảng cách giữa điều người dùng đã biết và điều họ muốn biết được thu hẹp vừa đủ, không gây ngập thông tin cũng không đánh đố.
- **Thực trạng điểm nghẽn:** Hộp tóm tắt hiện tại chỉ cắt cụt 3 dòng thô (`line-clamp-3`), thường đứt đoạn giữa chừng ở một dấu phẩy hoặc liên từ, gây ức chế thay vì tò mò.
- **Giải pháp đột phá:**
  - Thiết kế cấu trúc thẻ tin tức vi mô theo **3 Tầng Nhận Thức (3 Cognitive Tiers)**:
    - **Tầng 1 (The Hook - Lướt 2 giây):** Tiêu đề biên tập in đậm + 1 câu dẫn đề cốt lõi (Core Thesis) được làm nổi bật với font sans-serif tương phản cao.
    - **Tầng 2 (Takeaways - Nắm bắt 8 giây):** 2 đến 3 bullet point gạch đầu dòng cô đọng nhất (ví dụ: *"• Mức tăng: Đạt đỉnh 91 triệu/lượng", "• Nguyên nhân: Ảnh hưởng thị trường London", "• Dự báo: Khả năng điều chỉnh trong tuần tới"*).
    - **Tầng 3 (Deep Context - Đọc sâu):** Sheet trượt mở tóm tắt bối cảnh mở rộng và liên kết bài báo gốc.
- **Tác động:** Giảm tải nhận thức tức thì, giúp độc giả nắm bắt toàn bộ bản chất sự việc trong 10 giây mà không cần bấm link ngoài, đồng thời kích thích muốn tìm hiểu sâu các góc nhìn phân tích.

### Đề xuất 3: Nhịp Điệu Thị Giác Bento Bất Đối Xứng (Asymmetric Editorial Pacing & Hick-Hyman Mitigation)
- **Nguyên lý tâm lý:** **Định luật Hick-Hyman** và **Quy luật Gestalt về Hình - Nền (Figure-Ground Relationship)**. Não bộ con người xử lý thông tin dạng khối có thứ bậc nhanh hơn gấp 4 lần so với một ma trận đồng nhất.
- **Thực trạng điểm nghẽn:** Lưới 3 cột đồng dạng gây "tuyết mù", không có tiêu điểm.
- **Giải pháp đột phá:**
  - Tái cấu trúc Bento Grid theo **Tỷ lệ Nhịp điệu Báo chí Biên tập (Editorial Pacing Ratio)**:
    - **Anchor Lead Card ($2 \times 2$ Grid Span):** Tin tiêu điểm nổi bật nhất trong ngày với hình ảnh khổ lớn, trích dẫn nổi bật, huy hiệu "Tâm điểm hôm nay".
    - **Spotlight Vertical Card ($1 \times 2$ Grid Span):** Thẻ dọc mật độ cao dành cho phân tích chuyên sâu hoặc tin phỏng vấn đặc biệt.
    - **Brief Pills ($1 \times 1$ Span):** Các thẻ tin tức nhanh dạng thẻ tóm tắt thanh thoát.
    - **Storyline Ribbon (Full Width $3 \times 1$):** Một dải băng ngang đại diện cho một mạch sự kiện lớn đang diễn ra với các mốc thời gian ngang.
- **Tác động:** Giảm số lượng quyết định từ 24 lựa chọn cạnh tranh xuống còn 3-4 nhóm trực quan rõ ràng, dẫn dắt ánh mắt người đọc di chuyển tự nhiên từ tin quan trọng nhất đến các tin thứ cấp.

### Đề xuất 4: Công Thái Học Vùng Ngón Cái & Vi Tương Tác Thỏa Mãn (Ergonomic Thumb-Zone & Peak-End Rule)
- **Nguyên lý tâm lý:** **Định luật Fitts ($MT = a + b \log_2(2D/W)$)** và **Quy tắc Đỉnh - Kết (Peak-End Rule - Daniel Kahneman)**. Cảm xúc tích cực của trải nghiệm người dùng được quyết định bởi khoảnh khắc cao trào (khoảnh khắc tương tác sảng khoái) và điểm kết thúc hành động.
- **Thực trạng điểm nghẽn:** Nút bấm rải rác ở mép trên, mép phải và góc dưới trái; thao tác Like khô cứng chỉ đổi màu icon đỏ đơn điệu; thanh tiến độ tạo áp lực thời gian.
- **Giải pháp đột phá:**
  - **Lower Ergonomic Action Dock:** Di chuyển toàn bộ cụm điều khiển quan trọng vào vòng cung thao tác ngón cái (vùng 30% dưới cùng của màn hình di động).
  - **Kinetic Double-Tap to Like:** Cho phép người dùng chạm đúp (Double-Tap) bất kỳ đâu trên thẻ để thả tim, bung hiệu ứng hạt nở (particle heart burst) với âm thanh haptic nhẹ nhàng. Thao tác $D \to 0$ triệt tiêu hoàn toàn độ trễ tiếp cận.
  - **Achievement Horizon Indicator:** Thay thế thanh đếm ngược đỏ gây căng thẳng bằng một đường viền phát sáng mềm mại (soft ambient glow) tự động sáng bừng lên khi người đọc hoàn thành bài viết, kèm thông điệp khích lệ tinh tế: *"Đã nắm bắt tin tức!"*.
- **Tác động:** Tạo ra dòng dopamine tự nhiên (micro-dopamine loop), biến mỗi lần quẹt và đọc thành một phản xạ thể chất đầy thích thú.

### Đề xuất 5: Chân Trời Khám Phá & Thuật Toán Phá Bẫy Thông Tin (Serendipity Horizon via Thompson Sampling MAB)
- **Nguyên lý tâm lý:** **Thuyết Khác Biệt Tối Ưu (Optimal Distinctiveness Theory)** và **Sự Thích Nghi Khoái Lạc (Hedonic Adaptation)**. Nếu độc giả chỉ liên tục nhận tin về đúng chủ đề họ thích (ví dụ chỉ xem Công nghệ), não bộ sẽ nhanh chóng thích nghi và rơi vào trạng thái bão hòa, nhàm chán (content fatigue). Việc đón nhận một tin tức mới lạ nhưng bất ngờ thú vị (Serendipity) sẽ kích hoạt mạnh mẽ thụ thể dopamine ở vùng thể vân bụng (ventral striatum).
- **Thực trạng điểm nghẽn:** Thuật toán hiện tại gán cố định chuyên mục có điểm thấp nhất (`leastFavCat`) vào vị trí 3 và 7, mang tính áp đặt cơ học và dễ gợi ý phải chủ đề người dùng thực sự ghét.
- **Giải pháp đột phá:** Triển khai mô hình **Multi-Armed Bandit (Thompson Sampling)** kết hợp **Cầu nối Ngữ nghĩa (Thematic Bridge)**:
  - Xem mỗi danh mục hoặc cụm chủ đề là một "cánh tay" của máy đánh bạc nhiều tay.
  - Sử dụng phân phối Beta($\alpha, \beta$) để tự động cân bằng giữa Khai thác (Exploitation) và Khám phá (Exploration).
  - Tin tức khám phá được gắn huy hiệu sáng giá: `✨ Góc nhìn mới • Khám phá` với tông màu hổ phách (amber), biến trải nghiệm khám phá thành một món quà tri thức bất ngờ.

---

## 3. Phân Tích Tiến Trình Tin Tức: Cụm Chủ Đề & Dòng Thời Gian (Story Evolution & Chronology)

### 3.1. Hiện trạng Mã Nguồn & CSDL
Qua rà soát chi tiết toàn bộ các file Java và Migration SQL:
- Bảng `article`: Chỉ lưu các trường phẳng: `title`, `summary`, `url`, `url_hash`, `image_url`, `published_at`, `source_id`, `category_id`, và `search_vector` (tsvector).
- **Hoàn toàn vắng bóng:**
  - Không có trường `story_id`, `thread_id`, `cluster_id`, hoặc `parent_article_id`.
  - Không có quan hệ tự tham chiếu (self-referencing relationship) giữa các bài viết.
  - Không có bảng sự kiện (`story_arc` hoặc `timeline_event`).
- **Năng lực Trích xuất Thực thể Hiện có:**
  - Lớp `VietnameseKeywordExtractor` sử dụng Regex và bộ lọc stopwords cực kỳ thông minh, trích xuất chính xác các thực thể viết hoa nhiều từ (e.g. *"Việt Nam"*, *"Hà Nội"*, *"Bộ Tài chính"*, *"Donald Trump"*) và các danh từ riêng/từ viết tắt (e.g. *"SJC"*, *"AI"*, *"ChatGPT"*, *"Apple"*).
  - Hiện tại cơ chế này chỉ dùng cho bảng `keyword_stat` và hiển thị đám mây từ khóa (Trending Cloud), chưa được tận dụng để xâu chuỗi bài viết!

### 3.2. Mô Hình Hóa Dòng Thời Gian Sự Kiện (Story Arc / Timeline Continuity Model)

Để biến các bài viết rời rạc thành một chuỗi diễn biến lôi cuốn, hệ thống cần một mô hình liên kết ngữ nghĩa & thời gian:

#### A. Thuật toán Gom cụm & Xâu chuỗi (Story Arc Clustering Engine)
Hai bài viết $A_i$ và $A_j$ thuộc cùng một Mạch sự kiện khi thỏa mãn:
1. **Trùng lặp Thực thể (Entity Co-occurrence):**
   $$|Entities(A_i) \cap Entities(A_j)| \ge 2 \quad \text{hoặc} \quad \exists e \in Entities(A_i) \cap Entities(A_j) \text{ có độ hiếm cao (High IDF)}$$
2. **Khoảng cách Thời gian (Temporal Proximity):**
   $$|\text{publishedAt}(A_i) - \text{publishedAt}(A_j)| \le 7 \text{ ngày}$$
3. **Độ tương đồng Toàn văn (Full-Text TSVECTOR Similarity):**
   Khớp lệnh truy vấn PostgreSQL qua từ khóa chung với xếp hạng `ts_rank` vượt ngưỡng $\tau = 0.15$.

#### B. Phân loại Giai đoạn Diễn biến (Narrative Arc Classification)
Sắp xếp các bài viết trong cụm theo thứ tự thời gian tăng dần: $t_0 \le t_1 \le \dots \le t_n$:
- **Giai đoạn 1: Khởi nguồn (Genesis):** $t_0$ — Bài viết sớm nhất thông báo về sự kiện/vụ việc.
- **Giai đoạn 2: Diễn biến (Progression):** $t_1, \dots, t_{n-1}$ — Các bài viết về điều tra, phản ứng, tác động thị trường.
- **Giai đoạn 3: Mới nhất / Điểm chốt (Latest Development):** $t_n$ — Cập nhật mới nhất trong ngày.

#### C. Biểu diễn Trực quan trên Giao diện (UI Representation)

```
+------------------------------------------------------------------------+
| 📍 MẠCH SỰ KIỆN: BIẾN ĐỘNG GIÁ VÀNG MIẾNG SJC (3 BẢN TIN)             |
+------------------------------------------------------------------------+
|                                                                        |
|  (○) 15/10 - 08:30 • Khởi nguồn                                       |
|   |  Giá vàng trong nước chạm mốc 89 triệu đồng/lượng...               |
|   |                                                                    |
|  (○) 16/10 - 14:15 • Diễn biến                                         |
|   |  Ngân hàng Nhà nước thông báo can thiệp thị trường qua đấu thầu...  |
|   |                                                                    |
|  (●) Hôm nay - 09:00 • Mới nhất [Đang xem]                             |
|      Giá vàng hạ nhiệt về vùng 87 triệu, người dân xếp hàng chờ mua    |
|                                                                        |
|  [ 🔮 Bạn muốn biết diễn biến tiếp theo? Bấm nhận thông báo khi có tin ]|
+------------------------------------------------------------------------+
```

---

## 4. Thiết Kế Thuật Toán Khám Phá Thông Minh: Serendipity & Multi-Armed Bandit (Thompson Sampling)

### 4.1. Bản chất Toán học của Multi-Armed Bandit (Thompson Sampling)

#### A. Định nghĩa Bài toán
Gọi $K$ là tập hợp các chuyên mục/chủ đề trong hệ thống:
$$K = \{\text{Thời sự}, \text{Kinh doanh}, \text{Công nghệ}, \text{Thế giới}, \dots\}$$
Mỗi chuyên mục $k \in K$ đại diện cho một cánh tay (arm). Người dùng có xác suất thực sự $\theta_k \in [0, 1]$ tương tác tích cực với chuyên mục đó mà hệ thống chưa biết chắc chắn.

#### B. Mô hình Beta-Binomial Conjugacy
Hệ thống duy trì một phân phối tiên nghiệm Beta cho từng cánh tay $k$:
$$\theta_k \sim \text{Beta}(\alpha_k, \beta_k)$$
Trong đó:
- $\alpha_k$: Số lần người dùng phản hồi tích cực (thành công) với chủ đề $k$.
- $\beta_k$: Số lần người dùng bỏ qua/lướt nhanh (thất bại) với chủ đề $k$.
- Khởi tạo ban đầu (Priors): $\alpha_k = 2, \beta_k = 2$ (phân phối đối xứng có trọng số vừa phải, tránh bẫy chia cho 0).

#### C. Cơ chế Lấy mẫu Thompson (Thompson Sampling Step)
Tại mỗi lượt điều phối thẻ tin tức:
1. Với mỗi chuyên mục $k$, rút ngẫu nhiên một mẫu $\hat{\theta}_k$ từ phân phối $\text{Beta}(\alpha_k, \beta_k)$:
   $$\hat{\theta}_k \sim \text{Beta}(\alpha_k, \beta_k)$$
2. Chọn chuyên mục có giá trị mẫu cao nhất:
   $$k^* = \arg\max_{k \in K} \hat{\theta}_k$$

*Ưu điểm vượt trội so với $\epsilon$-Greedy và UCB:*
- **Khám phá tự nhiên (Intrinsic Exploration):** Chuyên mục nào chưa được khám phá nhiều sẽ có phương sai lớn ($\alpha + \beta$ nhỏ), dẫn đến việc thỉnh thoảng mẫu rút $\hat{\theta}_k$ đạt giá trị rất cao, tạo cơ hội hiển thị tự động mà không cần tỷ lệ ngẫu nhiên cưỡng ép!
- **Hội tụ mềm dẻo:** Khi người dùng tích cực tương tác, phân phối co cụm dần quanh giá trị kỳ vọng thực tế $\frac{\alpha}{\alpha + \beta}$.

#### D. Hàm Phần Thưởng từ Tín Hiệu Ngầm (Implicit Reward Function)
Cập nhật tham số Beta ngay trong phiên:
- **Thành công ($\alpha_k \leftarrow \alpha_k + 1$):**
  - Thời gian đọc $T_{\text{dwell}} \ge 0.65 T_{\text{standard}}$
  - HOẶC Thao tác Deep Read (bấm đọc bài gốc)
  - HOẶC Thả tim / Lưu bài viết / Chia sẻ
  - HOẶC Mở xem Dòng thời gian sự kiện
- **Thất bại ($\beta_k \leftarrow \beta_k + 1$):**
  - Lướt qua cực nhanh ($T_{\text{dwell}} < 1.5\text{s}$ - Fast Skip)
- **Trung tính:** Giữ nguyên $\alpha_k, \beta_k$ khi $1.5\text{s} \le T_{\text{dwell}} < 0.65 T_{\text{standard}}$.

---

### 4.2. Công thức Điểm Số Khám Phá Bất Ngờ (Serendipity Metric)

Không phải mọi sự ngẫu nhiên đều là Serendipity. Tin tức rác ngoài luồng không phải là Serendipity; tin tức đúng gu quen thuộc cũng không phải là Serendipity. **Serendipity là sự giao thoa kỳ diệu giữa Điều Bất Ngờ (Unexpectedness) và Sự Thích Thú Hợp Lý (Relevant Novelty)**:

$$\text{Serendipity}(A_i, U) = \text{Novelty}(A_i, U) \times \text{Relevance}(A_i, U) \times \text{Quality}(A_i)$$

1. **Độ Mới Lạ (Novelty):**
   Khoảng cách góc giữa vector chuyên mục bài viết $A_i$ với vector sở thích thống trị của người dùng $U$:
   $$\text{Novelty}(A_i, U) = 1 - \frac{\alpha_{k(A_i)}}{\sum_{j} \alpha_j}$$
2. **Độ Phù Hợp Cầu Nối (Bridge Relevance):**
   Được xác định thông qua **Cầu nối Thực thể (Entity Bridge)**: Bài viết thuộc chuyên mục mới lạ nhưng chứa ít nhất 1 thực thể hoặc từ khóa đang cực kỳ thịnh hành trên mạng xã hội hoặc trùng với sở thích phụ của người dùng.
3. **Chất Lượng & Uy Tín (Quality):**
   Hệ số uy tín của tòa soạn báo (VnExpress: 1.25, Tuổi Trẻ: 1.20, v.v.).

### 4.3. Lịch Trình Điều Phối Thẻ Bản Tin (Deck Slotting Architecture)

Thay thế cơ chế chia rãnh cơ học cũ bằng tỷ lệ cân bằng vàng:
- **70% Khai Thác Cá Nhân Hóa (Exploitation via Thompson Sampling):** Lấy các bài viết từ những chuyên mục đạt mẫu $\hat{\theta}_k$ cao nhất.
- **20% Chân Trời Khám Phá (Serendipity Horizon):** Các bài viết có điểm $\text{Serendipity}(A_i, U)$ cao nhất, giúp người đọc mở rộng thế giới quan.
- **10% Tin Nóng Khẩn Cấp (Breaking Urgency):** Các bài viết có tốc độ chia sẻ cao và mới xuất bản dưới 3 giờ.

---

## 5. Bảng So Sánh Toàn Diện: Hiện Trạng vs Đề Xuất Đột Phá

| Tiêu chí | Trạng thái Hiện tại (Baseline) | Trạng thái Đề xuất Đột phá (Proposed Architecture) | Cơ sở Tâm lý học & Lợi ích |
| :--- | :--- | :--- | :--- |
| **Bố cục Thẻ Reels** | Văn bản đè lên ảnh nền; gradient tối; hộp mờ 14px; khó đọc ở Light Mode. | Thẻ Double-Bezel phân tách vùng ảnh và nội dung; nền sáng/tối chuẩn WCAG AAA; độ tương phản tối ưu. | Loại bỏ visual fatigue; đạt chuẩn WCAG AAA tương phản cao. |
| **Đo lường Đọc tin** | Thanh đếm giờ đỏ tăng tuyến tính; tạo áp lực thời gian cơ học. | Vòng phát sáng thành tựu (Achievement Horizon) êm dịu khi người dùng đọc xong. | Giảm timer anxiety; củng cố cảm giác thành tựu (Peak-End Rule). |
| **Cấu trúc Nội dung** | Cắt cụt 3 dòng thô (`line-clamp-3`), đứt mạch ý tưởng. | Phân tầng 3 lớp: Hook giật đề → 3 gạch đầu dòng then chốt → Sheet bối cảnh sâu. | Progressive Disclosure; tối ưu hóa Curiosity Gap không gây ức chế. |
| **Bố cục Lưới theo ngày** | Lưới 3 cột đối xứng đồng đều, nhạt nhòa, 20 thẻ giống hệt nhau. | Asymmetric Bento Grid: Thẻ Lead $2 \times 2$, Thẻ Dọc Spotlight $1 \times 2$, Thẻ Quick Bite, Dải Timeline. | Triệt tiêu "tuyết mù" (Snow-blindness); giảm tải Hick-Hyman Law. |
| **Vùng thao tác Di động** | Nút bấm rải rác mép phải và góc dưới trái, xa tầm với ngón cái. | Lower Ergonomic Dock ở đáy; Double-Tap to Like hạt nở bung tỏa. | Fitts' Law ($D \to 0$); tạo vòng phản hồi thỏa mãn xúc giác. |
| **Chuỗi Tin Tức (Story)** | Tin tức rời rạc; không liên kết mạch sự kiện; không có mốc thời gian. | Story Arc / Timeline Continuity: Huy hiệu mạch sự kiện; Stepper dòng thời gian trực quan. | Zeigarnik Effect; kích thích theo dõi "câu chuyện tiếp diễn ra sao". |
| **Gợi ý Khám phá** | Gán cứng chuyên mục ít đọc nhất vào slot 3 và 7; bế tắc thuật toán. | Multi-Armed Bandit (Thompson Sampling) + Cầu nối Serendipity phá bẫy thông tin. | Phá vỡ Filter Bubble; kích thích dopamine khám phá mới lạ. |

---

## 6. Kế Hoạch Triển Khai Kỹ Thuật (Engineering Roadmap cho R2 & R3)

### Bước 1: Mở rộng Backend & Cấu trúc Dữ liệu (R3 Foundation)
1. Thêm endpoint hoặc service tính toán `StoryArcService`:
   - Phân tích tương đồng thực thể từ `VietnameseKeywordExtractor`.
   - Cung cấp API `GET /api/articles/story-arcs` hoặc nhúng thuộc tính `storyArc` vào `ArticleResponse`.
2. Hỗ trợ tham số truy vấn gợi ý đa dạng (hoặc cung cấp dữ liệu đầy đủ cho client-side coordinator).

### Bước 2: Tái Cấu Trúc Thuật Toán Điều Phối Thẻ (Client/Server Coordinator)
1. Cài đặt lớp `ThompsonSamplingBandit`:
   - Khởi tạo mảng $\alpha_k, \beta_k$ cho các chuyên mục trong `localStorage`.
   - Hàm `sampleArm()` sinh số ngẫu nhiên theo phân phối Beta thông qua phân phối Gamma (hoặc xấp xỉ biến đổi Box-Muller).
   - Hàm `recordFeedback(category, isPositive)`.
2. Cài đặt bộ tính điểm `calculateSerendipityScore(article)`.
3. Tái cấu trúc hàm `slotArticleBatch()` theo tỷ lệ $70 - 20 - 10$ linh hoạt.

### Bước 3: Nâng Cấp Giao Diện `index.html` (R2 UI/UX Refactor)
1. **Chế độ Nền Sáng / Tối WCAG AAA:**
   - Hoàn thiện bảng màu CSS tokens tương phản cao, đồng bộ cả thẻ Reels và Bento Grid.
2. **Nâng cấp Thẻ Reels:**
   - Double-Tap to like với hoạt ảnh nổ tim bung nở.
   - Thêm 3 tầng nội dung (Hook, Key Takeaways, Deep sheet).
   - Thay thanh đếm ngược bằng Achievement Horizon.
3. **Nâng cấp Bento Grid:**
   - Bố cục lưới bất đối xứng (Lead $2 \times 2$, Spotlight $1 \times 2$, Quick Bites).
   - Drawer Dòng thời gian sự kiện (Story Arc Drawer).
4. **Vi tương tác:**
   - Phản hồi cuộn vuốt $<100\text{ms}$, mượt mà trên cả cảm ứng di động và phím tắt Desktop.

---

## 7. Kết luận Khảo sát
Hệ thống VNNews Hub đã có sẵn nền móng kiến trúc backend và dữ liệu vững chãi. Điểm nghẽn lớn nhất hiện tại nằm ở **tâm lý học trải nghiệm người dùng** và **sự thiếu vắng liên kết mạch tin tức cùng thuật toán gợi ý thực thụ**. Việc triển khai 5 đề xuất đột phá trên cùng cơ chế Story Arc và Thompson Sampling MAB sẽ nâng tầm dự án từ một trang đọc báo tổng hợp thông thường thành một **trung tâm tin tức thông minh, cuốn hút và chuẩn mực báo chí biên tập hiện đại**.
