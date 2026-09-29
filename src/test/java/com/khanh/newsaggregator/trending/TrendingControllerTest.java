package com.khanh.newsaggregator.trending;

import com.khanh.newsaggregator.common.exception.GlobalExceptionHandler;
import com.khanh.newsaggregator.trending.dto.TrendingKeywordResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrendingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TrendingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrendingService trendingService;

    @MockitoBean
    private com.khanh.newsaggregator.auth.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.khanh.newsaggregator.auth.CustomUserDetailsService customUserDetailsService;

    @Test
    void getTrendingKeywords_shouldReturnKeywordList() throws Exception {
        List<TrendingKeywordResponse> mockKeywords = List.of(
                new TrendingKeywordResponse("Hà Nội", 15),
                new TrendingKeywordResponse("Apple", 12),
                new TrendingKeywordResponse("TP HCM", 10)
        );

        when(trendingService.getTrendingKeywords(10)).thenReturn(mockKeywords);

        mockMvc.perform(get("/api/trending/keywords?limit=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].keyword").value("Hà Nội"))
                .andExpect(jsonPath("$[0].count").value(15))
                .andExpect(jsonPath("$[1].keyword").value("Apple"))
                .andExpect(jsonPath("$[2].keyword").value("TP HCM"));
    }
}
