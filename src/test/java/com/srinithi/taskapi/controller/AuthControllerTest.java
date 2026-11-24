package com.srinithi.taskapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srinithi.taskapi.dto.AuthResponse;
import com.srinithi.taskapi.dto.LoginRequest;
import com.srinithi.taskapi.dto.RegisterRequest;
import com.srinithi.taskapi.dto.UserResponse;
import com.srinithi.taskapi.entity.Role;
import com.srinithi.taskapi.exception.UnauthorizedException;
import com.srinithi.taskapi.security.CustomUserDetailsService;
import com.srinithi.taskapi.security.JwtAuthFilter;
import com.srinithi.taskapi.security.JwtService;
import com.srinithi.taskapi.security.SecurityConfig;
import com.srinithi.taskapi.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Auth endpoints are public, but the real SecurityConfig/JwtAuthFilter are still
// imported so this slice behaves exactly like production - its dependencies
// (JwtService, CustomUserDetailsService) are mocked since no request here ever
// carries a token, so they're never actually invoked.
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void register_returns201_withCreatedUser() throws Exception {
        when(authService.register(any())).thenReturn(
                new UserResponse(1L, "new@example.com", "New Person", Role.USER, Instant.now()));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("new@example.com", "password123", "New Person"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_returns400_onBlankTitleAndShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("not-an-email", "short", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_returns200_withTokenAndRole() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("signed.jwt.token", Role.USER));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_returns401_onBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
