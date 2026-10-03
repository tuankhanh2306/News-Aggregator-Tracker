# Original User Request

## 2026-10-03T14:56:25Z

Phân tích sâu toàn diện các điểm nghẽn trải nghiệm của dự án VNNews Hub, đồng thời trực tiếp cải tiến giao diện UI/UX trực quan, thân thiện và triển khai các thuật toán gợi ý mới (Serendipity & Multi-Armed Bandit, Dòng thời gian sự kiện) nhằm kích thích sự tò mò, khám phá và giữ chân độc giả.

Working directory: e:\News\News-Aggregator-Tracker
Integrity mode: demo

## Requirements

### R1. Phân tích Chuyên sâu & Báo cáo Điểm nghẽn Trải nghiệm (Deep Audit Report)
- Đánh giá toàn diện hiện trạng giao diện, luồng đọc tin vi mô (One-card-per-screen) và dạng lưới theo ngày (Bento Grid).
- Xác định cụ thể ít nhất 5 cơ hội cải tiến đột phá về hành vi đọc, tải nhận thức (cognitive load) và cảm xúc thị giác của người dùng.

### R2. Cải tiến Trực tiếp Giao diện & Trải nghiệm Người dùng (UI/UX Refactor & Micro-interactions)
- Nâng cấp giao diện trực quan, tối ưu trải nghiệm vuốt chạm/lướt tin mượt mà với hiệu ứng chuyển cảnh tự nhiên, hỗ trợ hoàn hảo cả màn hình di động lẫn máy tính để bàn.
- Hoàn thiện hệ thống tương phản cao (High Contrast) đồng bộ trên cả nền sáng (Light Mode) và nền tối (Dark Mode).
- Thiết kế các vi tương tác (micro-interactions: thanh đo thời gian đọc tinh tế, hiệu ứng tim, phản hồi kéo thả/lướt) tạo cảm giác sảng khoái và kích thích tiếp tục đọc.

### R3. Triển khai Thuật toán Khám phá Thông minh (Serendipity & Multi-Armed Bandit)
- Bổ sung cơ chế cân bằng giữa khai thác chủ đề yêu thích (Exploitation) và mở rộng khám phá ngẫu nhiên thông minh (Exploration via Multi-Armed Bandit / Thompson Sampling), phá bỏ bẫy thông tin một chiều (Filter bubble).
- Tích hợp tính năng Dòng thời gian sự kiện (Story Arc / Timeline Continuity): Tự động phát hiện và liên kết các tin tức có cùng mạch diễn biến, kích thích tâm lý muốn theo dõi "câu chuyện tiếp diễn ra sao".

## Acceptance Criteria

### Audit & Documentation Criteria
- [ ] Báo cáo phân tích chuyên sâu (Audit Report) được lập chi tiết, chỉ rõ điểm nghẽn và nguyên lý tâm lý học hành vi đằng sau từng đề xuất cải tiến.

### Functional & UI/UX Criteria
- [ ] Giao diện người dùng được nâng cấp trực tiếp trong `src/main/resources/static/index.html`, giữ trọn phong cách báo chí biên tập tinh tế, không có yếu tố AI-slop.
- [ ] Trải nghiệm lướt thẻ phản hồi nhanh (<100ms), không giật lag khi cuộn liên tục.
- [ ] Chế độ Nền Sáng / Nền Tối chuyển đổi tức thì, độ tương phản văn bản đạt chuẩn WCAG AAA.

### Algorithmic & Quality Criteria
- [ ] Luồng điều phối bản tin tích hợp cơ chế Serendipity & Story Continuity, giúp người đọc vừa thấy tin yêu thích vừa được gợi mở các tin tức bất ngờ, hấp dẫn.
- [ ] Mã nguồn biên dịch thành công 100% qua `./mvnw test-compile` không có lỗi.
