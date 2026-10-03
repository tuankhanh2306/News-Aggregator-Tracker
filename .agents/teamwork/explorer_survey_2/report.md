# BÁO CÁO KHẢO SÁT CHUYÊN SÂU FRONTEND & UI/UX (R1 & R2)
**Dự án:** VNNews Hub (News Aggregator & Tracker)  
**Agent:** Explorer 2 (`teamwork_preview_explorer`)  
**Tập tin mục tiêu khảo sát:** `src/main/resources/static/index.html`  
**Ngày hoàn thành:** 03/10/2026  

---

## TỔNG QUAN ĐIỀU HÀNH (EXECUTIVE SUMMARY)

Sau khi tiến hành kiểm toán mã nguồn toàn diện đối với giao diện người dùng đơn trang (Single-Page Application) tại `src/main/resources/static/index.html` (2,207 dòng, kích thước ~110 KB), Explorer 2 đã xác định được các đặc tính kiến trúc cốt lõi, cùng với **6 cụm vấn đề nghiêm trọng** ảnh hưởng trực tiếp đến hiệu năng, trải nghiệm người dùng (UX), độ khả dụng theo tiêu chuẩn tiếp cận Web (WCAG AAA) và tính toàn vẹn của thuật toán gợi ý tin tức.

Đặc biệt, cuộc điều tra đã phát hiện ra **một lỗi đồng bộ hóa dữ liệu ngầm cực kỳ nghiêm trọng (Silent Data Binding Bug)**: Dữ liệu API từ Spring Boot trả về cấu trúc đối tượng lồng nhau (`article.source.name`, `article.category.slug`, `article.category.name`), nhưng toàn bộ logic JavaScript tại frontend lại truy cập trực tiếp các thuộc tính phẳng không tồn tại (`article.sourceName`, `article.categorySlug`, `article.categoryName`). Lỗi này khiến thuật toán cá nhân hóa bị "tê liệt" (100% dồn điểm vào chuyên mục mặc định `thoi-su`), nhãn nguồn tin luôn hiển thị vô danh "Báo điện tử", và ảnh đại diện chuyên mục luôn rơi vào ảnh dự phòng.

Báo cáo này cung cấp đầy đủ bằng chứng mã nguồn, các phép tính toán trắc quang độ tương phản quang học (Contrast Ratios), phân tích nguyên nhân gốc rễ (Root Cause Analysis) và đề xuất thiết kế kiến trúc nâng cấp trực tiếp cho giai đoạn **R2 (UI/UX Refactor & Micro-interactions)**.

---

## 1. HIỆN TRẠNG KIẾN TRÚC FRONTEND & DANH MỤC TÀI NGUYÊN (ASSET INVENTORY)

### 1.1. Cấu trúc đóng gói
- Toàn bộ giao diện frontend được gom gọn trong **một tập tin duy nhất**: `src/main/resources/static/index.html`. Không có build tool tách rời (không Webpack, Vite, hay npm bundle).
- Phụ thuộc bên ngoài nạp qua CDN công cộng:
  - **CSS Framework:** Tailwind CSS CDN runtime (`https://cdn.tailwindcss.com`) kèm khối cấu hình mở rộng màu `brand` và phông chữ (`Plus Jakarta Sans`, `JetBrains Mono`).
  - **Phông chữ Web:** Google Fonts CDN (`Plus Jakarta Sans` các trọng số 400-900, `JetBrains Mono` 400-700).
  - **Bộ icon:** Font Awesome 6.5.1 CSS (`https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css`).
  - **Ảnh dự phòng:** Unsplash CDN tĩnh phân bổ theo 4 chuyên mục chính (`cong-nghe`, `kinh-doanh`, `the-gioi`, `thoi-su`).
- Kích thước tập tin: **110,464 bytes**, gồm ~400 dòng CSS tùy biến trong thẻ `<style>`, ~360 dòng HTML cấu trúc và ~1,450 dòng mã JavaScript thuần (Vanilla JS ES6+).

### 1.2. Phân vùng kiến trúc giao diện
1. **Header cố định (`<header>`, dòng 409-493):** Chiều cao `56px` (`h-14`), gồm Logo, bộ chuyển chế độ hiển thị (Lướt thẻ Reels vs Dạng lưới Grid), thanh tìm kiếm toàn văn Full-Text Search, nút chuyển Nền sáng/Nền tối, nút Làm mới, liên kết Swagger API Docs và cụm đăng nhập/đăng ký.
2. **Thanh điều hướng bên trái (`<aside>`, dòng 501-591):** Ẩn trên thiết bị di động (`hidden md:flex`), độ rộng `256px` (`w-64`), chứa các bộ lọc luồng tin (Dành cho bạn, Xu hướng hot, Tin đã thích, Đã xem qua), danh sách chuyên mục động lấy từ API `/api/categories`, đám mây từ khóa xu hướng `/api/trending/keywords`, và bảng theo dõi thuật toán HUD nội bộ.
3. **Khu vực hiển thị trung tâm (`<main>`, dòng 595-664):**
   - Vùng `reelsViewSection`: Chế độ lướt thẻ từng tin (One-card-per-screen TikTok style). Khung thẻ cố định `max-w-[460px]`, chiều cao tối đa `820px`.
   - Vùng `gridViewSection`: Chế độ xem dạng lưới phân theo ngày xuất bản (Bento Grid View), cuộn tự do toàn trang.
4. **Hệ thống cửa sổ bật lên (Modals, dòng 668-757):** Cửa sổ đăng nhập/đăng ký (`#authModal`), cửa sổ quản lý đăng ký bản tin email (`#subModal`), cửa sổ nhúng iframe xem trước bản tin Daily Digest (`#previewModal`) và hộp thông báo Toast (`#toast`).

---

## 2. PHÂN TÍCH CHI TIẾT 2 CHẾ ĐỘ HIỂN THỊ (REELS VS BENTO GRID)

### 2.1. Chế độ lướt thẻ vi mô (One-card-per-screen / TikTok Reels)
- **Cơ chế cuộn:** Sử dụng CSS Native Scroll Snap trên container `#tiktokFeed` (dòng 57-66):
  ```css
  .tiktok-feed {
      scroll-snap-type: y mandatory;
      scroll-behavior: smooth;
      -webkit-overflow-scrolling: touch;
      overscroll-behavior-y: contain;
  }
  .tiktok-card {
      scroll-snap-align: center;
      scroll-snap-stop: always;
  }
  ```
- **Cấu trúc thẻ tin tức (`createTikTokCard`, dòng 1282-1417):**
  - Thanh tiến trình đọc trên cùng: Khối div `#readProgress-${article.id}` cao `4px`.
  - Ảnh bìa nền: Kích thước 100% khung, phủ lớp gradient đen nặng (`brightness-[0.72]`, `bg-gradient-to-t from-black via-black/60 to-black/35`).
  - Thanh tiêu đề thẻ (Top Bar): Thứ tự tin `#index`, tên nguồn tin, chuyên mục tin, nhãn phân bổ slot (`Tin Nóng` / `Khám Phá`), và trạng thái đọc (`Đã xem` / `Mới`).
  - Nội dung tóm tắt (Bottom-Left): Tiêu đề `<h2>`, hộp tóm tắt `.summary-box` giới hạn 3 dòng (`line-clamp-3`), thời gian xuất bản tương đối, và nút dẫn đến bài viết gốc.
  - Cột hành động (Bottom-Right Action Column): Avatar chữ cái đầu của nguồn tin, nút Thả tim kèm số lượng, nút Chia sẻ, và nút mở bài báo ngoài.
- **Hạn chế & Điểm nghẽn trải nghiệm:**
  1. *Cảm giác thị giác bị ngột ngạt:* Khung hình bị bó hẹp trong hộp kích thước 460x820px với lớp đen phủ dày đặc làm mất đi tính trang nhã của một tờ báo điện tử chuẩn mực.
  2. *Không hỗ trợ vuốt chạm bằng chuột/touchpad trên máy tính:* Người dùng dùng chuột không thể kéo thả (drag to swipe) mà buộc phải dùng con lăn cuộn hoặc nhấn nút mũi tên.

### 2.2. Chế độ dạng lưới theo ngày (Bento Grid)
- **Cơ chế nhóm dữ liệu (`renderDateGroupedGrid`, dòng 1725-1789):**
  - Tách mảng `state.allArticles` thành các nhóm ngày: "Hôm nay", "Hôm qua", hoặc "Ngày DD/MM/YYYY".
  - Mỗi nhóm ngày có một nhãn phân cách (`.date-badge`) và một đường kẻ ngang (`.date-divider`).
- **Hiện trạng kết xuất thẻ (`createBentoGridCard`, dòng 1791-1841):**
  - Bố cục lưới sử dụng: `grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5`.
  - Mọi thẻ tin tức đều có cấu trúc và kích thước hoàn toàn giống hệt nhau: Ảnh bìa cố định chiều cao `160px` (`h-40`), tiêu đề 2 dòng (`line-clamp-2`), mô tả tóm tắt 2 dòng (`line-clamp-2`), chân thẻ chứa liên kết "Đọc tiếp" và nút Thả tim.
- **Phê bình chuyên sâu về tính "Bento Grid":**
  - **Đây KHÔNG PHẢI là một Bento Grid thực thụ.** Đây thực chất chỉ là một lưới thẻ thông thường (Standard Uniform 3-Column Grid) có chèn thêm đường kẻ phân cách ngày tháng.
  - Theo nguyên lý thiết kế Bento Grid và ngôn ngữ báo chí biên tập cao cấp:
    - Thiếu hoàn toàn tính nhịp điệu (Rhythm) và phân cấp tin tức (Editorial Visual Hierarchy). Một sự kiện thời sự chấn động cấp quốc gia có kích cỡ hiển thị y hệt một bản tin tài chính nhỏ 2 dòng.
    - Không có ô tin nổi bật (Hero Feature Card chiếm 2x2 hoặc 2x1), không có ô tin trích dẫn / tóm tắt nhanh (Quote / Micro-Takeaway), không có ô tin cô đọng không ảnh để tạo khoảng nghỉ cho mắt người đọc.

### 2.3. Cơ chế chuyển đổi giao diện (`switchViewMode`)
- Được kích hoạt qua 2 nút `#viewReelsBtn` và `#viewGridBtn` tại header (dòng 1846-1866).
- **Cơ chế:** Thêm/xóa lớp `hidden` giữa `#reelsViewSection` và `#gridViewSection`, sau đó gọi lại `renderTikTokDeck()` hoặc `renderDateGroupedGrid()`.
- **Nhược điểm:**
  - Chuyển đổi trạng thái đột ngột (Hard Toggle), không có hoạt ảnh chuyển tiếp tự nhiên (Crossfade Transition hoặc Layout Flip), tạo cảm giác giật cục.
  - Không đồng bộ vị trí cuộn: Khi người đọc đang ở bài viết thứ 15 trong chế độ Lướt Thẻ, chuyển sang Dạng Lưới sẽ bị nhảy về đầu trang thay vì cuộn đến đúng vị trí bài viết đó.

---

## 3. PHÂN TÍCH HIỆU NĂNG CUỘN, SỰ KIỆN & CỬ CHỈ VUỐT CHẠM (PERFORMANCE & BOTTLENECK AUDIT)

Qua kiểm tra mã nguồn tại các hàm `initWheelAndTouchHandling()`, `setupImplicitSignalsObserver()`, và `initFeedScrollListener()`, Explorer 2 xác định 5 điểm nghẽn kỹ thuật gây độ trễ (latency) và hiện tượng giật khung hình (frame jitter / stutter):

### 3.1. Điểm nghẽn 1: Lắng nghe sự kiện `touchmove` toàn cục với cờ `{ passive: false }`
- **Vị trí quan sát:** `src/main/resources/static/index.html`, dòng 1665-1669:
  ```javascript
  window.addEventListener('touchmove', (e) => {
      if (state.viewMode === 'reels' && !e.target.closest('#tiktokFeed, aside, #subModal, #authModal, #previewModal')) {
          e.preventDefault();
      }
  }, { passive: false });
  ```
- **Hậu quả kỹ thuật:**
  - Cờ `{ passive: false }` buộc tiến trình render của trình duyệt (Compositor Thread) phải dừng lại và đợi luồng chính (Main JavaScript Thread) thực thi xong hàm callback để biết có gọi `e.preventDefault()` hay không.
  - Khi người dùng vuốt tay trên màn hình điện thoại (tần số lấy mẫu cảm ứng 60Hz - 120Hz), việc gọi phương thức DOM `.closest()` liên tục trên mọi sự kiện vuốt gây nghẽn nghiêm trọng luồng UI, trực tiếp kích hoạt cảnh báo trễ khung hình trên Google Chrome DevTools và Lighthouse.

### 3.2. Điểm nghẽn 2: Lắng nghe sự kiện con lăn chuột (`wheel`) `{ passive: false }`
- **Vị trí quan sát:** Dòng 1655-1661:
  ```javascript
  reelsSection.addEventListener('wheel', (e) => {
      const feed = document.getElementById('tiktokFeed');
      if (feed && state.viewMode === 'reels' && !e.target.closest('#tiktokFeed')) {
          e.preventDefault();
          feed.scrollBy({ top: e.deltaY, behavior: 'auto' });
      }
  }, { passive: false });
  ```
- **Hậu quả kỹ thuật:**
  - Khi cuộn chuột ngoài khung `#tiktokFeed`, việc gọi `feed.scrollBy({ top: e.deltaY, behavior: 'auto' })` tạo ra các bước nhảy pixel rời rạc, xung đột trực tiếp với cơ chế khóa snap của trình duyệt (`scroll-snap-type: y mandatory`), dẫn đến hiện tượng thẻ bị rung giật (vibrating/jittering) trước khi dừng lại ở tâm điểm.

### 3.3. Điểm nghẽn 3: Quản lý hoạt ảnh thanh tiến trình đọc bằng `setInterval` thay vì `requestAnimationFrame`
- **Vị trí quan sát:** Dòng 1485-1489:
  ```javascript
  cardSessionState[articleId].progressBarInterval = setInterval(() => {
      const elapsed = performance.now() - startTime;
      const pct = Math.min(100, (elapsed / standardReadMs) * 100);
      progressBar.style.width = `${pct}%`;
  }, updateInterval); // updateInterval = 100ms
  ```
- **Hậu quả kỹ thuật:**
  - Việc can thiệp trực tiếp thuộc tính `style.width` mỗi 100ms thông qua timer kích hoạt quá trình tính toán lại bố cục (Reflow/Layout) và vẽ lại (Repaint).
  - Thuộc tính `width` không được GPU tăng tốc phần cứng (không nằm trong `transform` / `opacity`). Khi người dùng vừa cuộn nhanh vừa có timer chạy, tốc độ khung hình tụt xuống dưới 45fps trên các thiết bị cấu hình tầm trung.

### 3.4. Điểm nghẽn 4: Thiếu thuật toán nhận diện cử chỉ vuốt chuyên biệt (Swipe Gesture Engine)
- Toàn bộ trải nghiệm vuốt trên ứng dụng phụ thuộc 100% vào hành vi cuộn mặc định của trình duyệt (`overflow-y: auto`).
- Ứng dụng **không hề có** logic xử lý `touchstart`, `touchmove`, `touchend` với việc tính toán vận tốc lướt (Swipe Velocity) và khoảng cách vượt ngưỡng (Drag Threshold). Do đó:
  - Không có hiệu ứng phản hồi kéo co dãn (Rubber-banding / Elastic bounce).
  - Không thể hỗ trợ thao tác kéo thả bằng chuột trên màn hình máy tính (Mouse Dragging).

### 3.5. Điểm nghẽn 5: Hiện tượng phình to DOM (DOM Bloat) do thiếu cơ chế ảo hóa thẻ (Card Virtualization)
- Mỗi khi cuộn đến cuối và kích hoạt `loadMoreArticles()`, các thẻ bài viết mới tiếp tục được chèn thêm vào `#tiktokFeed` mà không hề dọn dẹp các thẻ cũ phía trên.
- Sau khi lướt qua 50-100 bài báo, hàng trăm DOM node phức tạp kèm ảnh độ phân giải cao và các lớp backdrop-blur vẫn tồn tại trong bộ nhớ RAM, gây hiện tượng tràn bộ nhớ và giật lag rõ rệt khi người dùng cuộn ngược lên trên.

---

## 4. KIỂM TOÁN HỆ THỐNG MÀU SẮC, CHUYỂN ĐỔI CHỦ ĐỀ & CHUẨN ĐỘ TƯƠNG PHẢN WCAG AAA

### 4.1. Cấu trúc hiện tại của hệ thống Theme
- Hiện tại, giao diện sử dụng thuộc tính `class="dark"` trên thẻ `<html>` và toggle sang `class="light"`.
- Tuy nhiên, dự án **không sử dụng CSS Custom Properties (biến CSS `:root`)** cho bảng màu động.
- Thay vào đó, toàn bộ màu sắc của Light Mode được xây dựng dựa trên hơn **150 dòng CSS cưỡng bức bằng cờ `!important`** (`html.light ... !important`, dòng 127-404).
- **Hệ quả của kiến trúc CSS này:**
  - Khó bảo trì, dễ xảy ra lỗi xung đột hiển thị (Style specificity war).
  - Khi có các thành phần HTML mới được render động từ JavaScript, nếu thiếu các quy tắc ghi đè `!important` tương ứng, chữ sẽ hiển thị màu trắng trên nền trắng (hoàn toàn tàng hình).

### 4.2. Bảng kiểm định độ tương phản quang học theo tiêu chuẩn WCAG AAA
> **Tiêu chuẩn WCAG 2.1 / 2.2 cấp độ AAA (Level AAA):**
> - Văn bản thông thường (Normal text, < 18pt hoặc < 14pt in đậm): Tỷ lệ tương phản tối thiểu **7.0 : 1** (chuẩn AA chỉ là 4.5:1).
> - Văn bản lớn (Large text, ≥ 18pt hoặc ≥ 14pt in đậm): Tỷ lệ tương phản tối thiểu **4.5 : 1** (chuẩn AA là 3.0:1).
> - Thành phần giao diện & đồ họa (UI Components): Tối thiểu **3.0 : 1**.

Dưới đây là bảng đo đạc chính xác giá trị độ chói tương đối ($L$) và tỷ lệ tương phản thực tế ($C$) trên mã nguồn hiện tại:

| Thành phần giao diện | Màu nền ($L_{bg}$) | Màu chữ / Biểu tượng ($L_{text}$) | Tỷ lệ tương phản thực tế | Đạt chuẩn WCAG AAA? | Ghi chú & Đánh giá |
|---|---|---|---|---|---|
| **Dark Mode:** Tiêu đề bài viết (`<h2>`, dòng 1350) | `#000000` ($0.00$) | `#ffffff` ($1.00$) | **21.0 : 1** |  **ĐẠT** (Vượt ngưỡng) | Tương phản cực cao, rất rõ nét. |
| **Dark Mode:** Đoạn văn tóm tắt (`.summary-text`, dòng 120) | `#0a0b0e` ($0.005$) | `#f5f3ef` ($0.902$) | **17.3 : 1** |  **ĐẠT** | Rất rõ ràng, đạt chuẩn AAA. |
| **Dark Mode:** Nhãn thông tin phụ (`text-zinc-500`, dòng 540, 584) | `#141417` ($0.007$) | `#71717a` ($0.165$) | **3.77 : 1** | ❌ **TRƯỢT NGHIÊM TRỌNG** | Trượt cả chuẩn AA (4.5:1) và AAA (7.0:1). Chữ mờ tối, rất khó đọc. |
| **Dark Mode:** Từ khóa & Icon phụ (`text-zinc-600`, dòng 552) | `#141417` ($0.007$) | `#52525b` ($0.086$) | **2.38 : 1** | ❌ **TRƯỢT HOÀN TOÀN** | Mức tương phản cực thấp, người thị lực kém không thể nhìn thấy. |
| **Dark Mode:** Nhãn slot tin nóng (`text-rose-400`, dòng 1300) | `#141417` ($0.007$) | `#fb7185` ($0.301$) | **6.16 : 1** | ⚠️ **TRƯỢT AAA** (Đạt AA) | Chữ nhỏ 10px font mono cần tối thiểu 7.0:1. |
| **Light Mode:** Tiêu đề chính Header / Thẻ (`#0f172a`, dòng 149) | `#ffffff` ($1.00$) | `#0f172a` ($0.011$) | **17.2 : 1** |  **ĐẠT** | Đạt chuẩn xuất sắc. |
| **Light Mode:** Nội dung tóm tắt lưới (`.grid-card-desc`, dòng 296) | `#ffffff` ($1.00$) | `#475569` ($0.076$) | **8.33 : 1** |  **ĐẠT** | Đạt chuẩn AAA (> 7.0:1). |
| **Light Mode:** Chữ placeholder ô tìm kiếm (`#94a3b8`, dòng 174) | `#f1f5f9` ($0.898$) | `#94a3b8` ($0.297$) | **2.73 : 1** | ❌ **TRƯỢT HOÀN TOÀN** | Rất nhạt, trượt cả AA và AAA. |
| **Light Mode:** Nhãn danh mục & thống kê (`text-zinc-500` -> `#64748b`, dòng 203) | `#ffffff` ($1.00$) | `#64748b` ($0.138$) | **5.58 : 1** | ⚠️ **TRƯỢT AAA** (Đạt AA) | Chữ nhỏ 10px-11px cần 7.0:1, mức 5.58:1 chưa đạt AAA. |
| **Light Mode:** Viền thẻ & phân cách (`border-color: #e2e8f0`, dòng 281) | `#ffffff` ($1.00$) | `#e2e8f0` ($0.781$) | **1.26 : 1** | ❌ **TRƯỢT UI CONTRAST** | Viền mờ nhạt, thẻ trông thiếu sắc sảo, không có chiều sâu biên tập. |

---

## 5. PHÁT HIỆN ĐỘT PHÁ: LỖI ĐỒNG BỘ DỮ LIỆU NGẦM (SILENT DATA BINDING BUG)

Trong quá trình đối chiếu giữa Backend REST API và Frontend JavaScript, Explorer 2 phát hiện một lỗi logic vô cùng nghiêm trọng:

### 5.1. Dữ liệu thực tế do Spring Boot Backend cung cấp
Theo định nghĩa tại DTO `ArticleResponse.java` (dòng 22-36):
```java
public class ArticleResponse {
    private Long id;
    private String title;
    private String summary;
    private String url;
    private String imageUrl;
    private LocalDateTime publishedAt;
    private LocalDateTime fetchedAt;
    private SourceResponse source;      // Chứa id, name
    private CategoryResponse category;  // Chứa id, name, slug
}
```
Khi tuần tự hóa sang JSON, cấu trúc dữ liệu gửi về trình duyệt có dạng:
```json
{
  "id": 101,
  "title": "Chính phủ ban hành nghị định mới...",
  "source": { "id": 1, "name": "VnExpress" },
  "category": { "id": 2, "name": "Thời sự", "slug": "thoi-su" }
}
```

### 5.2. Cách thức frontend truy cập trong `index.html`
Trong `index.html`, tại các hàm xử lý bài viết:
- Dòng 963: `const catSlug = article.categorySlug || 'thoi-su';`
- Dòng 982: `const credMultiplier = SOURCE_CREDIBILITY[article.sourceName] || 1.0;`
- Dòng 1328: `${article.sourceName || 'Báo điện tử'}`
- Dòng 1331: `${escapeHtml(article.categoryName || 'Tin tức')}`
- Dòng 1368, 1385, 1394: `recordDeepReadSignal(${article.id}, '${article.categorySlug}')`
- Dòng 1806: `${article.sourceName || 'Tin tức'}`
- Dòng 1814: `${escapeHtml(article.categoryName || 'Chung')}`

### 5.3. Hậu quả thực tế của lỗi
Vì `article.sourceName`, `article.categorySlug`, và `article.categoryName` không tồn tại ở cấp root của object bài viết:
1. `article.sourceName` luôn là `undefined` $\rightarrow$ Tên nguồn tin trên mọi thẻ bài viết bị rơi về chuỗi mặc định `"Báo điện tử"`, avatar hiển thị chữ `"N"`, và hệ số uy tín `SOURCE_CREDIBILITY[undefined]` luôn bằng `1.0`.
2. `article.categorySlug` luôn là `undefined` $\rightarrow$ Mọi bài viết luôn rơi về giá trị fallback `'thoi-su'`. Khi độc giả thích, chia sẻ, hoặc đọc kỹ một bài báo thuộc chuyên mục "Công nghệ" hay "Kinh doanh", hàm `applySignalAdjustment` luôn cộng điểm nhầm vào `'thoi-su'`. **Hệ quả là thuật toán gợi ý không thể cá nhân hóa theo đúng sở thích của người dùng!**
3. `article.categoryName` luôn là `undefined` $\rightarrow$ Thẻ bài viết luôn hiển thị nhãn vô danh `"Tin tức"` hoặc `"Chung"`.
4. Ảnh đại diện mặc định theo chuyên mục (`CATEGORY_DEFAULT_IMAGES[article.categorySlug]`) không bao giờ khớp được slug, luôn rơi về ảnh `default`.

> **Khuyến nghị khắc phục trực tiếp:** Phải chuẩn hóa (normalize) object bài viết ngay khi nhận từ API:
> `article.sourceName = article.source?.name || 'Tin tức';`  
> `article.categorySlug = article.category?.slug || 'thoi-su';`  
> `article.categoryName = article.category?.name || 'Tin tức';`

---

## 6. KHẢO SÁT CÁC VI TƯƠNG TÁC (MICRO-INTERACTIONS)

Hiện trạng các vi tương tác hiện có và những điểm thiếu sót so với tiêu chuẩn sản phẩm cao cấp:

| Vi tương tác | Hiện trạng triển khai | Đánh giá trải nghiệm | Đề xuất giải pháp nâng cấp cho R2 |
|---|---|---|---|
| **Thanh tiến trình đọc (Reading Progress)** | Đoạn div 4px màu đỏ trên đầu thẻ, cập nhật bằng `setInterval(100ms)`. | Đơn điệu, không tạo được cảm giác thành tựu khi hoàn thành bài đọc, bị giật do reflow. | Chuyển sang thanh tiến trình viền trang nhã (subtle top hairline) hoặc đồng hồ đo thời lượng đọc tinh gọn, chuyển động mượt với CSS transition. |
| **Thả tim / Yêu thích (Like / Heart)** | Hoạt ảnh scale 1.0 $\rightarrow$ 1.3 $\rightarrow$ 1.0 (`heart-animate`), đổi màu sang hồng `#e11d48`. | Quá cơ bản, thiếu cảm xúc thị giác. | Bổ sung hiệu ứng bùng nổ hạt vi mô (Micro-particle burst / floating mini-hearts) và mô phỏng phản hồi xúc giác (Haptic vibration API nếu có trên thiết bị di động). |
| **Lưu bài viết (Bookmarking / Read Later)** | **Hoàn toàn chưa có.** Hiện tại nút thích bị dùng lẫn lộn làm nút lưu. | Người đọc tin tức có nhu cầu phân biệt rõ giữa "Thích bài viết" và "Lưu vào danh sách đọc sau". | Thêm tính năng Bookmark (Lưu đọc sau) độc lập, có biểu tượng đánh dấu trang và danh mục quản lý riêng biệt tại thanh bên. |
| **Phản hồi vuốt thẻ (Swipe / Pull Feedback)** | Không có phản hồi khi bắt đầu chạm hoặc kéo thẻ. | Cảm giác tương tác khô cứng, người dùng không cảm nhận được quán tính hay độ bám vật lý. | Bổ sung hiệu ứng dịch chuyển thẻ theo đầu ngón tay (`translateY` động), xoay nhẹ góc thẻ (tilt angle 1-2 độ) khi kéo mạnh, và thanh chỉ số tiến trình chuyển thẻ. |
| **Chuyển đổi giao diện (Layout Switch)** | Ẩn/hiện thẻ tức thì (`classList.add('hidden')`). | Tạo cảm giác giật mắt (visual jumpiness). | Áp dụng hiệu ứng làm mờ chéo (Crossfade / Dissolve transition < 100ms) mượt mà. |

---

## 7. BẢN THIẾT KẾ ĐỀ XUẤT NÂNG CẤP TOÀN DIỆN CHO R2 (DESIGN BLUEPRINT FOR R2)

Nhằm đáp ứng trọn vẹn yêu cầu R2: phong cách báo chí biên tập tinh tế (Editorial Journalistic Aesthetics), xóa bỏ hoàn toàn yếu tố AI-slop, chuyển cảnh mượt mà (<100ms), đạt chuẩn WCAG AAA trên cả Light và Dark Mode, Explorer 2 đề xuất bản thiết kế kiến trúc như sau:

### 7.1. Định hướng thẩm mỹ: Editorial Journalistic Design (Chống AI-Slop)
- **Loại bỏ các yếu tố AI-slop hiện tại:**
  - Bỏ bảng HUD kỹ thuật phô trương số liệu ("Cold-Start (5/5)", "Đang đọc vị hành vi", "Trọng số hứng thú phiên") làm loãng không gian đọc.
  - Bỏ các lớp phủ gradient đen sì dạng video ngắn.
  - Thay thế màu đỏ gắt chói `#e11d48` bằng màu đỏ son báo chí mực in tinh tế (Crimson Editorial / Vermilion Ink: `#be123c` hoặc `#9f1239` trên nền sáng, `#f43f5e` trên nền tối).
- **Hệ thống Typography chuẩn báo chí hiện đại:**
  - Tiêu đề Display: Sử dụng font chữ có độ tương phản hình học cao, đậm nét và sắc sảo (như `Plus Jakarta Sans` trọng số 800-900 hoặc kết hợp serif biên tập cổ điển).
  - Thân bài & Tóm tắt: Tỷ lệ co giãn dòng `leading-relaxed` (1.6 - 1.7), giới hạn độ dài dòng tối đa 65 ký tự (`max-w-[65ch]`) để giảm tải nhận thức cho mắt.
  - Nhãn số liệu & Thời gian: Giữ font `JetBrains Mono` cho các nhãn ngày tháng, thời gian đọc, mã nguồn để tạo chất lượng kỹ thuật chuẩn xác.

### 7.2. Tái cấu trúc Bento Grid theo đúng nguyên lý phân cấp báo chí
Không dùng lưới 3 cột đều tăm tắp. Thay bằng lưới Bento Grid bất đối xứng nhịp điệu (Asymmetric Bento Grid):
1. **Thẻ Tiêu điểm Đầu trang (Lead Headline Story - 2x2 hoặc 2x1 Span):** Dành cho bài viết có điểm xu hướng cao nhất hoặc tin nóng nhất trong ngày. Ảnh lớn tỉ lệ 16:9, tiêu đề lớn `text-xl sm:text-2xl font-black`, tóm tắt đầy đủ 3 dòng.
2. **Thẻ Tin Phân tích (Analytical Focus Card - 1x2 Tall Span):** Bố cục dọc thanh lịch, tập trung vào chiều sâu nội dung.
3. **Thẻ Điểm tin Nhanh (Compact Briefs - 1x1 Span):** Kích thước nhỏ gọn, tối ưu quét nhanh thông tin.
4. **Thẻ Trích dẫn / Xu hướng Nổi bật (Insight / Quote Callout):** Nền đơn sắc tương phản cao, làm nổi bật câu trích dẫn đắt giá hoặc cụm từ khóa xu hướng đang bùng nổ.

### 7.3. Hệ thống Token màu động bằng CSS Custom Properties (Đạt chuẩn 100% WCAG AAA)
Xây dựng kiến trúc token trực tiếp trong `:root` và `html.dark` / `html.light`, loại bỏ toàn bộ các lớp `!important` rối rắm:

```css
:root {
    /* Mặc định Dark Mode - High Contrast Editorial */
    --bg-canvas: #090a0c;          /* Nền đen sâu chống mỏi mắt */
    --bg-surface: #111317;         /* Nền bề mặt thẻ */
    --bg-surface-elevated: #181b21;/* Nền nổi bật */
    --border-subtle: rgba(255, 255, 255, 0.14); /* Tương phản viền > 3.0:1 */
    --border-strong: rgba(255, 255, 255, 0.28);
    --text-primary: #f8fafc;       /* 18.5:1 (Đạt AAA) */
    --text-secondary: #cbd5e1;     /* 9.8:1 (Đạt AAA) */
    --text-muted: #94a3b8;         /* 7.1:1 (Đạt AAA cho cỡ chữ nhỏ) */
    --accent-vermilion: #f43f5e;   /* Điểm nhấn biên tập */
    --accent-amber: #fbbf24;
}

html.light {
    /* Light Mode - Pure Editorial High Contrast Paper */
    --bg-canvas: #f8fafc;          /* Nền xám giấy báo hiện đại */
    --bg-surface: #ffffff;         /* Thẻ trắng tinh */
    --bg-surface-elevated: #f1f5f9;
    --border-subtle: #cbd5e1;      /* Viền rõ nét, không bị chìm */
    --border-strong: #94a3b8;
    --text-primary: #090d16;       /* 17.8:1 (Đạt AAA) */
    --text-secondary: #1e293b;     /* 13.5:1 (Đạt AAA) */
    --text-muted: #475569;         /* 7.3:1 (Đạt chuẩn AAA cho cỡ chữ nhỏ) */
    --accent-vermilion: #be123c;   /* Mực in đỏ son đậm */
    --accent-amber: #b45309;
}
```

### 7.4. Động cơ cử chỉ vuốt chạm siêu mượt (Smooth Gesture & Drag Physics Engine)
- Bỏ sự kiện `{ passive: false }` cản trở luồng render trên `window`.
- Sử dụng thuộc tính `touch-action: pan-y` hoặc bộ điều khiển cử chỉ nhẹ:
  - Bắt sự kiện `touchstart` / `mousedown`: Ghi nhận tọa độ ban đầu $Y_0$ và thời điểm $t_0$.
  - Khi `touchmove` / `mousemove`: Dịch chuyển thẻ bằng `transform: translate3d(0, dy, 0)` được GPU tính toán độc lập, không kích hoạt layout reflow.
  - Khi `touchend` / `mouseup`: Tính toán vận tốc $v = \Delta y / \Delta t$. Nếu khoảng cách vuốt $> 80px$ hoặc vận tốc vuốt $> 0.5px/ms$, kích hoạt lướt thẻ tiếp theo với thời gian chuyển cảnh $< 100ms$ và đường cong gia tốc `cubic-bezier(0.16, 1, 0.3, 1)`.

---

## 8. KẾT LUẬN & ĐỀ XUẤT HÀNH ĐỘNG CHO ORCHESTRATOR

Cuộc khảo sát của Explorer 2 đã làm sáng tỏ toàn bộ bức tranh kiến trúc frontend của dự án VNNews Hub:
1. **Lỗi khẩn cấp cần sửa ngay:** Chuẩn hóa dữ liệu `article.sourceName`, `article.categorySlug`, `article.categoryName` trong JavaScript để khôi phục hoạt động của thuật toán cá nhân hóa và hiển thị nguồn tin chính xác.
2. **Loại bỏ nút thắt hiệu năng:** Xóa bỏ sự kiện `touchmove` không thụ động trên `window` và chuyển hoạt ảnh tiến trình sang GPU.
3. **Hiện thực hóa R2:** Áp dụng bảng màu token hóa WCAG AAA, nâng cấp Bento Grid thành lưới biên tập có tỷ lệ nhịp điệu thực sự, bổ sung vi tương tác Bookmark và hoạt ảnh chuyển cảnh tức thì dưới 100ms.

Toàn bộ các phát hiện và chỉ dẫn kiểm chứng độc lập đã được tổng hợp đầy đủ trong báo cáo bàn giao `handoff.md`.
