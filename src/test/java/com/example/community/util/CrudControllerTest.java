package com.example.community.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CRUD 컨트롤러 테스트 추상 클래스
 * 공통 CRUD 테스트를 상속받아 사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class CrudControllerTest<CREATE_DTO, UPDATE_DTO> {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected ApiTestHelper api;

    @BeforeEach
    void setUpHelper() {
        api = new ApiTestHelper(mockMvc, objectMapper);
    }

    // ========== 하위 클래스에서 구현해야 할 메서드 ==========

    /**
     * 기본 URL 반환 (예: "/users", "/boards")
     */
    protected abstract String getBaseUrl();

    /**
     * 생성 요청용 샘플 DTO
     */
    protected abstract CREATE_DTO createSampleDto();

    /**
     * 수정 요청용 샘플 DTO (null 반환 시 수정 테스트 스킵)
     */
    protected abstract UPDATE_DTO updateSampleDto();

    /**
     * 테스트 데이터 초기화 (필요한 경우 오버라이드)
     * @return 생성된 엔티티의 ID
     */
    protected abstract Long setupTestData();

    // ========== 공통 CRUD 테스트 ==========

    @Test
    @DisplayName("목록 조회 성공")
    void findAll_success() throws Exception {
        // given
        setupTestData();

        // when & then
        api.get(getBaseUrl())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("단건 조회 성공")
    void findById_success() throws Exception {
        // given
        Long id = setupTestData();

        // when & then
        api.get(getBaseUrl() + "/" + id)
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("단건 조회 실패 - 존재하지 않는 ID")
    void findById_notFound() throws Exception {
        // when & then
        api.getNotFound(getBaseUrl() + "/99999");
    }

    @Test
    @DisplayName("생성 성공")
    void create_success() throws Exception {
        // given
        CREATE_DTO dto = createSampleDto();

        // when & then
        api.post(getBaseUrl(), dto)
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("수정 성공")
    void update_success() throws Exception {
        // given
        Long id = setupTestData();
        UPDATE_DTO dto = updateSampleDto();

        if (dto == null) {
            return; // 수정 기능이 없는 경우 스킵
        }

        // when & then
        api.put(getBaseUrl() + "/" + id, dto);
    }

    @Test
    @DisplayName("삭제 성공")
    void delete_success() throws Exception {
        // given
        Long id = setupTestData();

        // when & then
        api.delete(getBaseUrl() + "/" + id);
    }
}
