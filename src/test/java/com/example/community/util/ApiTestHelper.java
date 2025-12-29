package com.example.community.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 테스트 헬퍼 유틸리티
 * 반복되는 MockMvc 호출을 간소화한다.
 */
public class ApiTestHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public ApiTestHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    // ========== GET ==========

    /**
     * GET 요청 (200 OK 기대)
     */
    public ResultActions get(String url) throws Exception {
        return mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * GET 요청 (404 Not Found 기대)
     */
    public ResultActions getNotFound(String url) throws Exception {
        return mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ========== POST ==========

    /**
     * POST 요청 (201 Created 기대)
     */
    public ResultActions post(String url, Object body) throws Exception {
        return mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body)))
                .andExpect(status().isCreated());
    }

    // ========== PUT ==========

    /**
     * PUT 요청 (200 OK 기대)
     */
    public ResultActions put(String url, Object body) throws Exception {
        return mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body)))
                .andExpect(status().isOk());
    }

    // ========== DELETE ==========

    /**
     * DELETE 요청 (204 No Content 기대)
     */
    public ResultActions delete(String url) throws Exception {
        return mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ========== 유틸리티 ==========

    public String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
