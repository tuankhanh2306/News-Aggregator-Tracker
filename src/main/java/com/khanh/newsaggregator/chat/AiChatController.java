package com.khanh.newsaggregator.chat;

import com.khanh.newsaggregator.chat.dto.ChatRequest;
import com.khanh.newsaggregator.chat.dto.ChatResponse;
import com.khanh.newsaggregator.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chatbox", description = "Trợ lý Đọc báo Thông minh (Hybrid RAG: Gemini / Groq / Local Editorial)")
public class AiChatController {

    private final AiChatService aiChatService;

    @Operation(summary = "Đặt câu hỏi cho Trợ lý AI dựa trên ngữ cảnh bản tin đang đọc và dữ liệu báo chí trong hệ thống")
    @PostMapping("/ask")
    public ApiResponse<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = aiChatService.askAssistant(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Kiểm tra trạng thái động cơ AI Chatbox và gợi ý câu hỏi mặc định")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(aiChatService.getAssistantStatus());
    }
}
