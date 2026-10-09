package com.khanh.newsaggregator.chat.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    /**
     * Nội dung câu trả lời đã được biên tập.
     */
    private String reply;

    /**
     * Động cơ xử lý: GEMINI, GROQ, hoặc LOCAL_EDITORIAL.
     */
    private String engine;

    /**
     * Danh sách các bài báo thực tế trong DB được trích dẫn.
     */
    private List<ChatArticleRef> relatedArticles;

    /**
     * Các câu hỏi gợi ý tiếp theo phù hợp ngữ cảnh.
     */
    private List<String> suggestedPrompts;
}
