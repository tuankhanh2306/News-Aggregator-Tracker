package com.khanh.newsaggregator.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.newsaggregator.auth.dto.AuthResponse;
import com.khanh.newsaggregator.auth.dto.LoginRequest;
import com.khanh.newsaggregator.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void register_validRequest_shouldReturn201Created() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .expiresInMs(86400000)
                .userId(1L)
                .email("user@example.com")
                .role("ROLE_USER")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void login_validRequest_shouldReturn200Ok() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .expiresInMs(86400000)
                .userId(1L)
                .email("user@example.com")
                .role("ROLE_USER")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void register_invalidEmail_shouldReturn400BadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
