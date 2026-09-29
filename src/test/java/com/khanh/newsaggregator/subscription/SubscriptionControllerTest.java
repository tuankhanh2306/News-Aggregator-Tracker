package com.khanh.newsaggregator.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.newsaggregator.auth.CustomUserDetailsService;
import com.khanh.newsaggregator.auth.JwtTokenProvider;
import com.khanh.newsaggregator.subscription.dto.SubscriptionRequest;
import com.khanh.newsaggregator.subscription.dto.SubscriptionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "subscriber@example.com")
    void getSubscriptions_shouldReturn200Ok() throws Exception {
        SubscriptionResponse response = SubscriptionResponse.builder()
                .categoryId(1L)
                .categoryName("Thời sự")
                .categorySlug("thoi-su")
                .frequency("DAILY")
                .createdAt(LocalDateTime.now())
                .build();

        when(subscriptionService.getSubscriptions("subscriber@example.com"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/subscriptions")
                        .principal(() -> "subscriber@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("Thời sự"));
    }

    @Test
    void subscribe_validRequest_shouldReturn201Created() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .categoryId(1L)
                .frequency("DAILY")
                .build();

        SubscriptionResponse response = SubscriptionResponse.builder()
                .categoryId(1L)
                .categoryName("Thời sự")
                .categorySlug("thoi-su")
                .frequency("DAILY")
                .createdAt(LocalDateTime.now())
                .build();

        when(subscriptionService.subscribe(eq("subscriber@example.com"), any(SubscriptionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/subscriptions")
                        .principal(() -> "subscriber@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Thời sự"));
    }

    @Test
    void unsubscribe_validId_shouldReturn204NoContent() throws Exception {
        doNothing().when(subscriptionService).unsubscribe("subscriber@example.com", 1L);

        mockMvc.perform(delete("/api/subscriptions/1")
                        .principal(() -> "subscriber@example.com"))
                .andExpect(status().isNoContent());
    }
}
