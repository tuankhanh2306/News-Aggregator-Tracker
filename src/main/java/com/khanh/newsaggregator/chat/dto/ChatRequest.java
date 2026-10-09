package com.khanh.newsaggregator.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    @Size(max = 600, message = "Câu hỏi tối đa 600 ký tự")
    private String message;

    /**
     * ID bài báo mà độc giả đang dừng xem trên màn hình (nếu có).
     */
    private Long currentArticleId;

    /**
     * ID chuyên mục đang được lọc trên trang (nếu có).
     */
    private Long categoryId;
}
