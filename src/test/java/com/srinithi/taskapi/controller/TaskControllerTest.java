package com.srinithi.taskapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srinithi.taskapi.dto.TaskCreateRequest;
import com.srinithi.taskapi.dto.TaskResponse;
import com.srinithi.taskapi.entity.Status;
import com.srinithi.taskapi.exception.NotFoundException;
import com.srinithi.taskapi.security.CustomUserDetailsService;
import com.srinithi.taskapi.security.JwtAuthFilter;
import com.srinithi.taskapi.security.JwtService;
import com.srinithi.taskapi.security.SecurityConfig;
import com.srinithi.taskapi.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void create_returns401_whenNoAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest("Title", "desc"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void create_returns201_whenAuthenticated() throws Exception {
        when(taskService.create(any())).thenReturn(
                new TaskResponse(1L, "Title", "desc", Status.OPEN, Instant.now(), 10L));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskCreateRequest("Title", "desc"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser
    void getOne_returns404_whenNotFoundOrNotOwned() throws Exception {
        when(taskService.getOne(anyLong())).thenThrow(new NotFoundException("Task not found"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    @WithMockUser
    void list_returns200_withPagedContent() throws Exception {
        Page<TaskResponse> page = new PageImpl<>(
                List.of(new TaskResponse(1L, "A task", null, Status.OPEN, Instant.now(), 10L)),
                PageRequest.of(0, 5), 1);
        when(taskService.list(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/tasks").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("A task"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
