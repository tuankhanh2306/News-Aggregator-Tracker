package com.khanh.newsaggregator.article;

import com.khanh.newsaggregator.article.dto.ArticleResponse;
import com.khanh.newsaggregator.common.dto.PageResponse;
import com.khanh.newsaggregator.common.exception.GlobalExceptionHandler;
import com.khanh.newsaggregator.common.exception.ResourceNotFoundException;
import com.khanh.newsaggregator.source.dto.SourceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private com.khanh.newsaggregator.auth.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.khanh.newsaggregator.auth.CustomUserDetailsService customUserDetailsService;

    @Test
    void getArticles_shouldReturnPageResponse() throws Exception {
        ArticleResponse article = ArticleResponse.builder()
                .id(1L)
                .title("Tin tức mẫu hôm nay")
                .summary("Tóm tắt nội dung bài viết.")
                .url("https://vnexpress.net/tin-tuc-mau.html")
                .publishedAt(LocalDateTime.now())
                .source(SourceResponse.builder().id(1L).name("VnExpress").build())
                .build();

        PageResponse<ArticleResponse> pageResponse = PageResponse.<ArticleResponse>builder()
                .content(List.of(article))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(articleService.getArticles(any(), any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Tin tức mẫu hôm nay"))
                .andExpect(jsonPath("$.content[0].source.name").value("VnExpress"));
    }

    @Test
    void getArticleById_whenFound_shouldReturnArticle() throws Exception {
        ArticleResponse article = ArticleResponse.builder()
                .id(10L)
                .title("Chi tiết bài viết")
                .summary("Nội dung chi tiết...")
                .url("https://tuoitre.vn/chi-tiet.html")
                .publishedAt(LocalDateTime.now())
                .build();

        when(articleService.getArticleById(10L)).thenReturn(article);

        mockMvc.perform(get("/api/articles/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Chi tiết bài viết"));
    }

    @Test
    void getArticleById_whenNotFound_shouldReturn404Json() throws Exception {
        when(articleService.getArticleById(999L))
                .thenThrow(new ResourceNotFoundException("Article", "id", 999L));

        mockMvc.perform(get("/api/articles/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Article not found with id: '999'"))
                .andExpect(jsonPath("$.path").value("/api/articles/999"));
    }

    @Test
    void getArticleById_withInvalidIdFormat_shouldReturn400Json() throws Exception {
        mockMvc.perform(get("/api/articles/not-a-number")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/articles/not-a-number"));
    }
}
