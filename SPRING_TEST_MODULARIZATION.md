# Spring 테스트 모듈화 가이드

> REST API 테스트의 중복을 줄이고 효율적으로 관리하는 3가지 방법

## 문서 정보

| 항목 | 내용 |
|------|------|
| **레벨** | 중급 |
| **예상 읽기 시간** | 20분 |
| **선행 지식** | Spring Boot 테스트 기초, JUnit 5, MockMvc |
| **최종 업데이트** | 2025년 1월 |

### 관련 문서
- [INDEX.md](INDEX.md) - 전체 문서 가이드
- [GITHUB_ACTIONS_TUTORIAL.md](GITHUB_ACTIONS_TUTORIAL.md) - CI에서 테스트 자동화
- [SPRING_DOCKER_SETUP.md](SPRING_DOCKER_SETUP.md) - 테스트 환경 설정

---

## 목차

1. [개요](#1-개요)
2. [방법 1: 테스트 헬퍼 유틸리티](#2-방법-1-테스트-헬퍼-유틸리티)
3. [방법 2: 추상 클래스 상속](#3-방법-2-추상-클래스-상속)
4. [방법 3: ParameterizedTest](#4-방법-3-parameterizedtest)
5. [비교 및 선택 가이드](#5-비교-및-선택-가이드)

---

## 1. 개요

### 왜 테스트 모듈화가 필요한가?

REST API 테스트는 비슷한 패턴이 반복된다.

```java
// UserControllerTest.java
@Test
void 유저_목록_조회() throws Exception {
    mockMvc.perform(get("/users")
           .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());
}

// BoardControllerTest.java
@Test
void 게시글_목록_조회() throws Exception {
    mockMvc.perform(get("/boards")
           .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());
}

// CommentControllerTest.java
@Test
void 댓글_목록_조회() throws Exception {
    mockMvc.perform(get("/comments")
           .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());
}
```

**문제점:**
- 거의 동일한 코드가 컨트롤러마다 반복
- URL만 다르고 나머지는 복붙
- 수정할 때 모든 파일 찾아서 고쳐야 함

**해결:** 모듈화로 공통 로직 재사용

---

## 2. 방법 1: 테스트 헬퍼 유틸리티

### 개념

자주 쓰는 MockMvc 호출을 메서드로 추출하여 재사용한다.

### 전체 코드

```java
package com.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ApiTestHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public ApiTestHelper(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.objectMapper = new ObjectMapper();
    }

    // ========== GET ==========

    /**
     * GET 요청 (200 OK 기대)
     */
    public ResultActions get(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * GET 요청 (404 Not Found 기대)
     */
    public ResultActions getNotFound(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ========== POST ==========

    /**
     * POST 요청 (201 Created 기대)
     */
    public ResultActions post(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)))
                .andExpect(status().isCreated());
    }

    /**
     * POST 요청 (400 Bad Request 기대)
     */
    public ResultActions postBadRequest(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)))
                .andExpect(status().isBadRequest());
    }

    // ========== PUT ==========

    /**
     * PUT 요청 (200 OK 기대)
     */
    public ResultActions put(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)))
                .andExpect(status().isOk());
    }

    // ========== PATCH ==========

    /**
     * PATCH 요청 (200 OK 기대)
     */
    public ResultActions patch(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)))
                .andExpect(status().isOk());
    }

    // ========== DELETE ==========

    /**
     * DELETE 요청 (204 No Content 기대)
     */
    public ResultActions delete(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    /**
     * DELETE 요청 (200 OK 기대 - 삭제된 데이터 반환하는 경우)
     */
    public ResultActions deleteWithResponse(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ========== 유틸리티 ==========

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
```

### 사용 예시

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ApiTestHelper api;

    @BeforeEach
    void setUp() {
        api = new ApiTestHelper(mockMvc);
    }

    @Test
    void 유저_목록_조회() throws Exception {
        api.get("/users")
           .andExpect(jsonPath("$").isArray());
    }

    @Test
    void 유저_단건_조회() throws Exception {
        api.get("/users/1")
           .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void 존재하지_않는_유저_조회() throws Exception {
        api.getNotFound("/users/9999");
    }

    @Test
    void 유저_생성() throws Exception {
        UserCreateRequest request = new UserCreateRequest("홍길동", "hong@email.com");

        api.post("/users", request)
           .andExpect(jsonPath("$.id").exists())
           .andExpect(jsonPath("$.name").value("홍길동"));
    }

    @Test
    void 유저_생성_유효성_실패() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest("", ""); // 빈 값

        api.postBadRequest("/users", invalidRequest);
    }

    @Test
    void 유저_삭제() throws Exception {
        api.delete("/users/1");
    }
}
```

### 장단점

| 장점 | 단점 |
|------|------|
| 도입이 쉬움 | 테스트 구조 자체는 여전히 반복 |
| 기존 테스트에 점진적 적용 가능 | HTTP 메서드별 코드만 줄어듦 |
| 유연한 커스터마이징 | CRUD 전체 흐름 강제 불가 |

---

## 3. 방법 2: 추상 클래스 상속

### 개념

CRUD 공통 테스트를 추상 클래스로 정의하고, 각 컨트롤러 테스트가 상속받는다.

### 전체 코드

```java
package com.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class CrudControllerTest<CREATE_DTO, UPDATE_DTO> {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

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
     * 수정 요청용 샘플 DTO
     */
    protected abstract UPDATE_DTO updateSampleDto();

    /**
     * 테스트에 사용할 존재하는 ID
     */
    protected abstract Long getExistingId();

    // ========== 공통 CRUD 테스트 ==========

    @Test
    void 목록_조회_성공() throws Exception {
        mockMvc.perform(get(getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void 단건_조회_성공() throws Exception {
        mockMvc.perform(get(getBaseUrl() + "/" + getExistingId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(getExistingId()));
    }

    @Test
    void 단건_조회_실패_존재하지_않는_ID() throws Exception {
        mockMvc.perform(get(getBaseUrl() + "/99999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void 생성_성공() throws Exception {
        mockMvc.perform(post(getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createSampleDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void 수정_성공() throws Exception {
        mockMvc.perform(put(getBaseUrl() + "/" + getExistingId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateSampleDto())))
                .andExpect(status().isOk());
    }

    @Test
    void 삭제_성공() throws Exception {
        mockMvc.perform(delete(getBaseUrl() + "/" + getExistingId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ========== 유틸리티 메서드 (하위 클래스에서 사용 가능) ==========

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
```

### 각 컨트롤러 테스트

```java
// UserControllerTest.java
class UserControllerTest extends CrudControllerTest<UserCreateDto, UserUpdateDto> {

    @Override
    protected String getBaseUrl() {
        return "/users";
    }

    @Override
    protected UserCreateDto createSampleDto() {
        return new UserCreateDto("홍길동", "hong@email.com");
    }

    @Override
    protected UserUpdateDto updateSampleDto() {
        return new UserUpdateDto("김철수", "kim@email.com");
    }

    @Override
    protected Long getExistingId() {
        return 1L;
    }

    // 필요시 추가 테스트 작성
    @Test
    void 이메일_중복_체크() throws Exception {
        // User 전용 테스트
    }
}
```

```java
// BoardControllerTest.java
class BoardControllerTest extends CrudControllerTest<BoardCreateDto, BoardUpdateDto> {

    @Override
    protected String getBaseUrl() {
        return "/boards";
    }

    @Override
    protected BoardCreateDto createSampleDto() {
        return new BoardCreateDto("제목", "내용", 1L);
    }

    @Override
    protected BoardUpdateDto updateSampleDto() {
        return new BoardUpdateDto("수정된 제목", "수정된 내용");
    }

    @Override
    protected Long getExistingId() {
        return 1L;
    }

    // 필요시 추가 테스트 작성
    @Test
    void 게시글_검색() throws Exception {
        // Board 전용 테스트
    }
}
```

```java
// CommentControllerTest.java
class CommentControllerTest extends CrudControllerTest<CommentCreateDto, CommentUpdateDto> {

    @Override
    protected String getBaseUrl() {
        return "/comments";
    }

    @Override
    protected CommentCreateDto createSampleDto() {
        return new CommentCreateDto("댓글 내용", 1L, 1L);
    }

    @Override
    protected CommentUpdateDto updateSampleDto() {
        return new CommentUpdateDto("수정된 댓글");
    }

    @Override
    protected Long getExistingId() {
        return 1L;
    }
}
```

### 장단점

| 장점 | 단점 |
|------|------|
| CRUD 테스트 자동 상속 | 상속 구조로 인한 복잡성 |
| 테스트 누락 방지 | CRUD가 아닌 API에는 부적합 |
| 일관된 테스트 구조 강제 | 상속은 하나만 가능 (Java 제약) |
| 새 컨트롤러 추가 시 빠름 | 추상 클래스 변경 시 전체 영향 |

---

## 4. 방법 3: ParameterizedTest

### 개념

같은 테스트 로직을 여러 데이터로 반복 실행한다.

### @CsvSource 예시

```java
@SpringBootTest
@AutoConfigureMockMvc
class ApiHealthTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 모든 목록 조회 API가 200 OK를 반환하는지 테스트
     */
    @ParameterizedTest(name = "{0} 엔드포인트 정상 응답")
    @CsvSource({
        "/users,       200",
        "/boards,      200",
        "/comments,    200",
        "/notifications, 200"
    })
    void 목록_API_정상_응답(String url, int expectedStatus) throws Exception {
        mockMvc.perform(get(url))
               .andExpect(status().is(expectedStatus));
    }

    /**
     * 인증이 필요한 API가 401을 반환하는지 테스트
     */
    @ParameterizedTest(name = "{0} 인증 필요")
    @CsvSource({
        "/admin/users",
        "/admin/settings",
        "/admin/logs"
    })
    void 인증_없이_접근_불가(String url) throws Exception {
        mockMvc.perform(get(url))
               .andExpect(status().isUnauthorized());
    }
}
```

### @MethodSource 예시 (복잡한 데이터)

```java
@SpringBootTest
@AutoConfigureMockMvc
class CrudApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 테스트 데이터 제공 메서드
     */
    static Stream<Arguments> crudTestCases() {
        return Stream.of(
            Arguments.of("/users", new UserCreateDto("홍길동", "hong@email.com")),
            Arguments.of("/boards", new BoardCreateDto("제목", "내용", 1L)),
            Arguments.of("/comments", new CommentCreateDto("댓글", 1L, 1L))
        );
    }

    @ParameterizedTest(name = "{0} 생성 테스트")
    @MethodSource("crudTestCases")
    void 리소스_생성_성공(String url, Object createDto) throws Exception {
        mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated());
    }

    /**
     * 유효성 검증 실패 케이스
     */
    static Stream<Arguments> validationFailCases() {
        return Stream.of(
            Arguments.of("/users", new UserCreateDto("", ""), "이름과 이메일 필수"),
            Arguments.of("/users", new UserCreateDto("홍길동", "invalid-email"), "이메일 형식"),
            Arguments.of("/boards", new BoardCreateDto("", "", null), "제목과 내용 필수")
        );
    }

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("validationFailCases")
    void 유효성_검증_실패(String url, Object invalidDto, String description) throws Exception {
        mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}
```

### 장단점

| 장점 | 단점 |
|------|------|
| 테스트 케이스 추가가 쉬움 | 복잡한 로직에는 부적합 |
| 한 메서드로 여러 케이스 커버 | 디버깅 시 어떤 케이스인지 찾기 어려움 |
| 테스트 결과가 깔끔하게 표시 | @MethodSource 사용 시 코드 분리됨 |

---

## 5. 방법 비교

### 한눈에 비교

| 기준 | 헬퍼 유틸리티 | 추상 클래스 상속 | ParameterizedTest |
|------|--------------|-----------------|-------------------|
| **난이도** | 쉬움 | 중간 | 쉬움 |
| **재사용성** | 중간 | 높음 | 낮음 |
| **강제성** | 없음 | 높음 (상속 필수) | 없음 |
| **유연성** | 높음 | 낮음 | 중간 |
| **적합한 상황** | 범용 | CRUD API | 동일 패턴 반복 |

### 어떤 상황에 무엇을 쓸까?

```
┌─────────────────────────────────────────────────────────────┐
│  Q1. CRUD API가 대부분인가?                                  │
│      YES → 추상 클래스 상속 고려                              │
│      NO  → Q2로                                             │
├─────────────────────────────────────────────────────────────┤
│  Q2. 같은 테스트를 여러 URL/데이터로 반복하는가?               │
│      YES → ParameterizedTest                                │
│      NO  → Q3로                                             │
├─────────────────────────────────────────────────────────────┤
│  Q3. MockMvc 호출 코드가 반복되는가?                          │
│      YES → 헬퍼 유틸리티                                     │
│      NO  → 그냥 작성                                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. 권장 조합

실제 프로젝트에서는 **조합해서 사용**한다.

### 추천 구조

```
src/test/java/
├── util/
│   ├── ApiTestHelper.java           # 방법 1: 공통 유틸리티
│   └── CrudControllerTest.java      # 방법 2: CRUD 추상 클래스
├── controller/
│   ├── UserControllerTest.java      # CrudControllerTest 상속
│   ├── BoardControllerTest.java     # CrudControllerTest 상속
│   └── SearchControllerTest.java    # 일반 테스트 (CRUD 아님)
└── integration/
    └── ApiHealthTest.java           # 방법 3: 전체 API 상태 체크
```

### 조합 예시

```java
// CRUD API: 추상 클래스 상속 + 헬퍼 유틸리티 조합
class UserControllerTest extends CrudControllerTest<UserCreateDto, UserUpdateDto> {

    private ApiTestHelper api;

    @BeforeEach
    void setUp() {
        api = new ApiTestHelper(mockMvc);
    }

    @Override
    protected String getBaseUrl() { return "/users"; }

    // ... 추상 메서드 구현 ...

    // 상속받은 CRUD 테스트 외에 추가 테스트
    @Test
    void 이메일_중복_확인() throws Exception {
        api.get("/users/check-email?email=test@test.com")
           .andExpect(jsonPath("$.available").value(true));
    }
}
```

```java
// 전체 API 상태 체크: ParameterizedTest
class ApiHealthTest {

    @ParameterizedTest
    @CsvSource({"/users", "/boards", "/comments"})
    void 모든_목록_API_정상(String url) throws Exception {
        mockMvc.perform(get(url))
               .andExpect(status().isOk());
    }
}
```

---

## 정리

| 방법 | 언제 쓰나 | 코드 예시 |
|------|----------|----------|
| **헬퍼 유틸리티** | MockMvc 호출 간소화 | `api.get("/users")` |
| **추상 클래스** | CRUD 테스트 강제/자동화 | `extends CrudControllerTest` |
| **ParameterizedTest** | 같은 테스트, 다른 데이터 | `@CsvSource({...})` |

**처음 시작한다면:** 헬퍼 유틸리티부터 도입 → 필요에 따라 확장

---

## 테스트 실행 속도 최적화

### 슬라이스 테스트 사용

```java
// 전체 컨텍스트 로드 (느림)
@SpringBootTest

// 웹 레이어만 로드 (빠름)
@WebMvcTest(UserController.class)

// JPA 레이어만 로드 (빠름)
@DataJpaTest
```

### 병렬 실행

```properties
# src/test/resources/junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
```

### 테스트 시간 비교

| 설정 | 테스트 10개 실행 시간 |
|------|---------------------|
| @SpringBootTest | ~15초 |
| @WebMvcTest | ~5초 |
| @WebMvcTest + 병렬 | ~2초 |

---

## 다음 단계

- [GitHub Actions에서 테스트 자동화](GITHUB_ACTIONS_TUTORIAL.md)
- [Docker 환경에서 테스트 실행](SPRING_DOCKER_SETUP.md)
- [데이터베이스 선택 가이드](DATABASE_SERVICE_COMPARISON.md)
