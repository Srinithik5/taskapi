package com.srinithi.taskapi.controller;

import com.srinithi.taskapi.dto.UserResponse;
import com.srinithi.taskapi.entity.Role;
import com.srinithi.taskapi.security.CustomUserDetailsService;
import com.srinithi.taskapi.security.JwtAuthFilter;
import com.srinithi.taskapi.security.JwtService;
import com.srinithi.taskapi.security.SecurityConfig;
import com.srinithi.taskapi.service.TaskService;
import com.srinithi.taskapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The point of this class: prove hasRole("ADMIN") in SecurityConfig is actually
// enforced, not just assumed - same table of checks as the manual Phase 6 curl tests,
// now automated so a future change can't silently break it.
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TaskService taskService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listUsers_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listUsers_returns403_forRegularUser() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_returns200_forAdmin() throws Exception {
        Page<UserResponse> page = new PageImpl<>(
                List.of(new UserResponse(1L, "a@example.com", "A", Role.USER, Instant.now())));
        when(userService.listAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("a@example.com"));
    }
}
