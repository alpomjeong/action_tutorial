# Spring Boot 3.x Swagger/OpenAPI 적용 가이드

> Spring Boot 3.x 프로젝트에 Swagger(OpenAPI 3.0)를 적용하는 완벽 가이드

## 문서 정보

| 항목 | 내용 |
|------|------|
| **레벨** | 초급 ~ 중급 |
| **예상 읽기 시간** | 35분 |
| **선행 지식** | Spring Boot 기초, REST API 이해 |
| **최종 업데이트** | 2025년 1월 |

### 관련 문서
- [INDEX.md](INDEX.md) - 전체 문서 가이드
- [SPRING_DOCKER_SETUP.md](SPRING_DOCKER_SETUP.md) - Docker 환경 설정
- [GITHUB_ACTIONS_TUTORIAL.md](GITHUB_ACTIONS_TUTORIAL.md) - CI/CD 설정

---

## 목차

1. [라이브러리 선택](#1-라이브러리-선택)
2. [기본 설정](#2-기본-설정)
3. [OpenAPI 설정 클래스](#3-openapi-설정-클래스)
4. [컨트롤러 어노테이션](#4-컨트롤러-어노테이션)
5. [모델/DTO 어노테이션](#5-모델dto-어노테이션)
6. [응답 문서화](#6-응답-문서화)
7. [보안 설정](#7-보안-설정)
8. [API 그룹화](#8-api-그룹화)
9. [환경별 설정](#9-환경별-설정)
10. [Spring Security 연동](#10-spring-security-연동)
11. [실전 예제](#11-실전-예제)
12. [문제 해결](#12-문제-해결)
13. [빠른 참조 치트시트](#13-빠른-참조-치트시트)

---

## 1. 라이브러리 선택

### Springfox vs Springdoc-OpenAPI

| 항목 | Springfox | Springdoc-OpenAPI |
|------|-----------|-------------------|
| **상태** | 사실상 중단 (2020년 이후 업데이트 없음) | 활발히 유지보수 중 |
| **Spring Boot 3.x 지원** | 불가 (Jakarta EE 미지원) | 완벽 지원 |
| **OpenAPI 버전** | OpenAPI 2.0 (Swagger) | OpenAPI 3.0/3.1 |
| **권장** | 신규 프로젝트에 사용 금지 | **권장** |

**결론:** Spring Boot 3.x에서는 반드시 **springdoc-openapi**를 사용해야 합니다.

### 버전 호환성

| Spring Boot 버전 | Springdoc-OpenAPI 버전 |
|-----------------|----------------------|
| 3.4.x ~ 3.5.x | 2.7.x ~ 2.8.x |
| 3.2.x ~ 3.3.x | 2.3.x ~ 2.6.x |
| 3.0.x ~ 3.1.x | 2.0.x ~ 2.2.x |
| 2.7.x | 1.6.x ~ 1.8.x |

**현재 최신 버전:** `2.8.14` (2025년 1월 기준)

---

## 2. 기본 설정

### 2.1 의존성 추가

**Gradle (build.gradle):**
```groovy
dependencies {
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14'
}

// Spring Boot 3.2+ 필수: 파라미터 이름 보존
tasks.withType(JavaCompile) {
    options.compilerArgs << '-parameters'
}
```

**Gradle Kotlin DSL (build.gradle.kts):**
```kotlin
dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
```

**Maven (pom.xml):**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.14</version>
</dependency>

<!-- Spring Boot 3.2+ 필수 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

### 2.2 기본 설정 (application.yml)

```yaml
springdoc:
  # API 문서 엔드포인트
  api-docs:
    enabled: true
    path: /v3/api-docs

  # Swagger UI 설정
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    try-it-out-enabled: true           # "Try it out" 버튼 활성화
    operations-sorter: alpha           # 알파벳순 정렬
    tags-sorter: alpha                 # 태그 알파벳순 정렬
    display-request-duration: true     # 요청 시간 표시
    filter: true                       # 검색 필터 활성화
    persist-authorization: true        # 인증 정보 유지

  # 스캔 범위 제한
  packages-to-scan: com.example.controller
  paths-to-match: /api/**
```

### 2.3 기본 URL 정리

| 엔드포인트 | URL | 설명 |
|-----------|-----|------|
| Swagger UI | `/swagger-ui.html` | 대화형 API 문서 |
| Swagger UI (대체) | `/swagger-ui/index.html` | 직접 접근 |
| OpenAPI JSON | `/v3/api-docs` | OpenAPI 스펙 (JSON) |
| OpenAPI YAML | `/v3/api-docs.yaml` | OpenAPI 스펙 (YAML) |

---

## 3. OpenAPI 설정 클래스

### 3.1 기본 설정

```java
package com.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Community API")
                .version(appVersion)
                .description("커뮤니티 서비스 REST API 문서")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@example.com")
                    .url("https://example.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("개발 서버"),
                new Server()
                    .url("https://api.example.com")
                    .description("운영 서버")
            ));
    }
}
```

### 3.2 어노테이션 기반 설정 (대안)

```java
package com.example.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Community API",
        version = "1.0.0",
        description = "커뮤니티 서비스 REST API 문서",
        contact = @Contact(
            name = "API Support",
            email = "support@example.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "개발 서버"),
        @Server(url = "https://api.example.com", description = "운영 서버")
    }
)
public class OpenApiConfig {
}
```

---

## 4. 컨트롤러 어노테이션

### 4.1 @Tag - 컨트롤러 그룹화

```java
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {
    // ...
}
```

### 4.2 @Operation - 메서드 설명

```java
import io.swagger.v3.oas.annotations.Operation;

@GetMapping("/{id}")
@Operation(
    summary = "사용자 조회",
    description = "ID로 특정 사용자를 조회합니다. 존재하지 않으면 404를 반환합니다.",
    operationId = "getUserById"
)
public UserResponse getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```

### 4.3 @Parameter - 파라미터 설명

```java
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

@GetMapping("/search")
@Operation(summary = "사용자 검색")
public List<UserResponse> searchUsers(
    @Parameter(description = "검색어", required = true, example = "홍길동")
    @RequestParam String query,

    @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
    @RequestParam(defaultValue = "0") int page,

    @Parameter(description = "페이지 크기", example = "10")
    @RequestParam(defaultValue = "10") int size
) {
    return userService.search(query, page, size);
}
```

### 4.4 @Hidden - 문서에서 숨기기

```java
import io.swagger.v3.oas.annotations.Hidden;

// 컨트롤러 전체 숨기기
@Hidden
@RestController
@RequestMapping("/api/internal")
public class InternalController {
    // ...
}

// 특정 메서드만 숨기기
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Hidden
    @GetMapping("/debug")
    public String debug() {
        return "debug info";
    }
}
```

---

## 5. 모델/DTO 어노테이션

### 5.1 @Schema - 모델 필드 설명

```java
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "사용자 생성 요청")
public class CreateUserRequest {

    @Schema(description = "사용자 이름", example = "홍길동", minLength = 2, maxLength = 50)
    @NotBlank
    @Size(min = 2, max = 50)
    private String name;

    @Schema(description = "이메일 주소", example = "hong@example.com", format = "email")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "비밀번호 (최소 8자)", example = "SecureP@ss123", minLength = 8,
            accessMode = Schema.AccessMode.WRITE_ONLY)
    @Size(min = 8)
    private String password;

    // getters, setters...
}
```

### 5.2 응답 DTO

```java
@Schema(description = "사용자 응답")
public class UserResponse {

    @Schema(description = "사용자 ID", example = "12345", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    @Schema(description = "이메일 주소", example = "hong@example.com")
    private String email;

    @Schema(description = "사용자 역할", example = "USER")
    private UserRole role;

    @Schema(description = "계정 활성화 여부", example = "true")
    private Boolean active;

    @Schema(description = "생성일시", example = "2025-01-15T10:30:00Z")
    private LocalDateTime createdAt;

    // getters, setters...
}
```

### 5.3 Enum 문서화

```java
@Schema(description = "사용자 역할")
public enum UserRole {

    @Schema(description = "일반 사용자")
    USER,

    @Schema(description = "관리자")
    ADMIN,

    @Schema(description = "모더레이터")
    MODERATOR
}
```

### 5.4 @ArraySchema - 배열/리스트

```java
import io.swagger.v3.oas.annotations.media.ArraySchema;

@Schema(description = "사용자 목록 응답")
public class UserListResponse {

    @ArraySchema(
        schema = @Schema(implementation = UserResponse.class),
        minItems = 0
    )
    private List<UserResponse> users;

    @Schema(description = "전체 개수", example = "100")
    private Long totalCount;
}
```

---

## 6. 응답 문서화

### 6.1 @ApiResponses - 응답 코드 정의

```java
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@GetMapping("/{id}")
@Operation(summary = "사용자 조회")
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UserResponse.class)
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "사용자를 찾을 수 없음",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
})
public UserResponse getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```

### 6.2 @ExampleObject - 구체적인 예시

```java
import io.swagger.v3.oas.annotations.media.ExampleObject;

@PostMapping
@Operation(summary = "사용자 생성")
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "생성할 사용자 정보",
    required = true,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = CreateUserRequest.class),
        examples = {
            @ExampleObject(
                name = "기본 사용자",
                summary = "필수 필드만 포함",
                value = """
                    {
                        "name": "홍길동",
                        "email": "hong@example.com",
                        "password": "SecureP@ss123"
                    }
                    """
            ),
            @ExampleObject(
                name = "관리자 사용자",
                summary = "역할 지정 포함",
                value = """
                    {
                        "name": "관리자",
                        "email": "admin@example.com",
                        "password": "AdminP@ss123",
                        "role": "ADMIN"
                    }
                    """
            )
        }
    )
)
@ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "생성 성공",
        content = @Content(
            schema = @Schema(implementation = UserResponse.class),
            examples = @ExampleObject(
                value = """
                    {
                        "id": 1,
                        "name": "홍길동",
                        "email": "hong@example.com",
                        "role": "USER",
                        "active": true,
                        "createdAt": "2025-01-15T10:30:00Z"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "유효성 검사 실패",
        content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                value = """
                    {
                        "error": "Validation Failed",
                        "message": "이메일 형식이 올바르지 않습니다",
                        "timestamp": "2025-01-15T10:30:00Z"
                    }
                    """
            )
        )
    )
})
public ResponseEntity<UserResponse> createUser(
    @RequestBody CreateUserRequest request
) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.create(request));
}
```

### 6.3 공통 에러 응답 클래스

```java
@Schema(description = "에러 응답")
public class ErrorResponse {

    @Schema(description = "에러 타입", example = "NOT_FOUND")
    private String error;

    @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다")
    private String message;

    @Schema(description = "발생 시각", example = "2025-01-15T10:30:00Z")
    private Instant timestamp;

    @Schema(description = "요청 경로", example = "/api/users/123")
    private String path;

    @ArraySchema(schema = @Schema(implementation = FieldError.class))
    @Schema(description = "필드별 에러 (유효성 검사 실패 시)")
    private List<FieldError> fieldErrors;

    // getters, setters...
}

@Schema(description = "필드 에러")
public class FieldError {

    @Schema(description = "필드명", example = "email")
    private String field;

    @Schema(description = "에러 메시지", example = "올바른 이메일 형식이 아닙니다")
    private String message;
}
```

---

## 7. 보안 설정

### 7.1 JWT Bearer 인증

**프로그래밍 방식:**
```java
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiSecurityConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI securedOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Secured API")
                .version("1.0.0"))
            // 전역 보안 요구사항 추가
            .addSecurityItem(new SecurityRequirement()
                .addList(SECURITY_SCHEME_NAME))
            // 보안 스키마 정의
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요")));
    }
}
```

**어노테이션 방식:**
```java
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT 토큰을 입력하세요"
)
public class OpenApiSecurityConfig {
}
```

### 7.2 특정 엔드포인트에만 보안 적용

```java
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User")
public class UserController {

    // 인증 필요
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse getMyInfo() {
        return userService.getCurrentUser();
    }

    // 인증 불필요 (public)
    @GetMapping("/public/count")
    @Operation(summary = "전체 사용자 수 (공개)")
    @SecurityRequirements  // 빈 어노테이션 = 보안 없음
    public Long getUserCount() {
        return userService.count();
    }

    // 여러 보안 스키마 중 하나 (OR)
    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 삭제")
    @SecurityRequirements({
        @SecurityRequirement(name = "bearerAuth"),
        @SecurityRequirement(name = "apiKey")
    })
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

### 7.3 API Key 인증

```java
@Bean
public OpenAPI apiKeyOpenAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("apiKey", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-KEY")
                .description("API Key 인증")))
        .addSecurityItem(new SecurityRequirement().addList("apiKey"));
}
```

### 7.4 OAuth2 설정

```java
@Bean
public OpenAPI oauth2OpenAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("oauth2", new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("OAuth2 인증")
                .flows(new OAuthFlows()
                    .authorizationCode(new OAuthFlow()
                        .authorizationUrl("https://auth.example.com/authorize")
                        .tokenUrl("https://auth.example.com/token")
                        .scopes(new Scopes()
                            .addString("read", "읽기 권한")
                            .addString("write", "쓰기 권한"))))))
        .addSecurityItem(new SecurityRequirement().addList("oauth2"));
}
```

---

## 8. API 그룹화

### 8.1 경로 기반 그룹화

```java
import org.springdoc.core.models.GroupedOpenApi;

@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public-api")
            .displayName("공개 API")
            .pathsToMatch("/api/public/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin-api")
            .displayName("관리자 API")
            .pathsToMatch("/api/admin/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("user-api")
            .displayName("사용자 API")
            .pathsToMatch("/api/users/**", "/api/boards/**", "/api/comments/**")
            .pathsToExclude("/api/users/internal/**")
            .build();
    }
}
```

### 8.2 패키지 기반 그룹화

```java
@Bean
public GroupedOpenApi v1Api() {
    return GroupedOpenApi.builder()
        .group("v1")
        .displayName("API v1")
        .packagesToScan("com.example.controller.v1")
        .build();
}

@Bean
public GroupedOpenApi v2Api() {
    return GroupedOpenApi.builder()
        .group("v2")
        .displayName("API v2")
        .packagesToScan("com.example.controller.v2")
        .build();
}
```

### 8.3 application.yml로 그룹 설정

```yaml
springdoc:
  group-configs:
    - group: public
      display-name: 공개 API
      paths-to-match: /api/public/**
    - group: admin
      display-name: 관리자 API
      paths-to-match: /api/admin/**
    - group: user
      display-name: 사용자 API
      paths-to-match: /api/users/**, /api/boards/**
```

그룹별 접근 URL:
- `/v3/api-docs/public`
- `/v3/api-docs/admin`
- `/swagger-ui.html` → 드롭다운에서 그룹 선택

---

## 9. 환경별 설정

### 9.1 개발/운영 환경 분리

**application-dev.yml (개발):**
```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    try-it-out-enabled: true
```

**application-prod.yml (운영):**
```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

### 9.2 프로파일 기반 설정 클래스

```java
@Configuration
@Profile({"dev", "local", "test"})
public class SwaggerConfig {

    @Bean
    public OpenAPI devOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Community API (Development)")
                .version("1.0.0-SNAPSHOT")
                .description("개발 환경 API 문서"));
    }
}
```

### 9.3 조건부 빈 로딩

```java
@Configuration
@ConditionalOnProperty(
    name = "springdoc.swagger-ui.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class SwaggerConfig {
    // Swagger가 활성화된 경우에만 로드
}
```

---

## 10. Spring Security 연동

### 10.1 Swagger 엔드포인트 허용

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {
        "/v3/api-docs/**",
        "/v3/api-docs.yaml",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-resources/**",
        "/webjars/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Swagger 엔드포인트 허용
                .requestMatchers(SWAGGER_WHITELIST).permitAll()
                // 공개 API
                .requestMatchers("/api/public/**").permitAll()
                // 관리자 API
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(SWAGGER_WHITELIST)
            );

        return http.build();
    }
}
```

### 10.2 CORS 설정

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v3/api-docs/**")
            .allowedOrigins("*")
            .allowedMethods("GET");
        registry.addMapping("/swagger-ui/**")
            .allowedOrigins("*")
            .allowedMethods("GET");
    }
}
```

---

## 11. 실전 예제

### 11.1 완전한 컨트롤러 예제

```java
package com.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "사용자 목록 조회", description = "페이지네이션을 지원하는 사용자 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public PageResponse<UserResponse> getUsers(
        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "페이지 크기", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        return userService.findAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "사용자 단건 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public UserResponse getUser(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable Long id
    ) {
        return userService.findById(id);
    }

    @PostMapping
    @Operation(summary = "사용자 생성")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검사 실패")
    })
    public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "사용자 수정")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "사용자 없음"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public UserResponse updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 11.2 완전한 설정 파일

**OpenApiConfig.java:**
```java
package com.example.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile({"dev", "local"})
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Community API")
                .version(appVersion)
                .description("커뮤니티 서비스 REST API")
                .contact(new Contact()
                    .name("개발팀")
                    .email("dev@example.com")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("로컬"),
                new Server().url("https://dev-api.example.com").description("개발")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 토큰")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .displayName("공개 API")
            .pathsToMatch("/api/**")
            .pathsToExclude("/api/admin/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .displayName("관리자 API")
            .pathsToMatch("/api/admin/**")
            .build();
    }
}
```

---

## 12. 문제 해결

### 문제 1: 파라미터가 arg0, arg1로 표시됨

**원인:** Spring Boot 3.2+ 파라미터 이름 발견 방식 변경

**해결:**
```groovy
// Gradle
tasks.withType(JavaCompile) {
    options.compilerArgs << '-parameters'
}
```

```xml
<!-- Maven -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

### 문제 2: Swagger UI 401/403 에러

**원인:** Spring Security가 Swagger 엔드포인트 차단

**해결:** SecurityConfig에서 Swagger 경로 허용 (섹션 10.1 참조)

### 문제 3: CORS 에러

**해결:** CorsConfig 추가 (섹션 10.2 참조)

### 문제 4: 운영 환경에서 Swagger 노출

**해결:** application-prod.yml에서 비활성화
```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

### 문제 5: Springfox에서 마이그레이션

| Springfox (구버전) | Springdoc (신버전) |
|-------------------|-------------------|
| `@Api` | `@Tag` |
| `@ApiOperation` | `@Operation` |
| `@ApiParam` | `@Parameter` |
| `@ApiModel` | `@Schema` |
| `@ApiModelProperty` | `@Schema` |
| `@ApiIgnore` | `@Hidden` |
| `@ApiResponse(code=404)` | `@ApiResponse(responseCode="404")` |

---

## 13. 빠른 참조 치트시트

### 의존성
```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14'
```

### 기본 URL
```
Swagger UI:  http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
```

### 컨트롤러 어노테이션
```java
@Tag(name = "User", description = "설명")        // 컨트롤러 그룹
@Operation(summary = "요약", description = "설명") // 메서드 설명
@Parameter(description = "설명", example = "값")   // 파라미터
@Hidden                                          // 문서에서 숨김
@SecurityRequirement(name = "bearerAuth")        // 보안 적용
```

### 모델 어노테이션
```java
@Schema(description = "설명", example = "값")              // 필드 설명
@Schema(accessMode = Schema.AccessMode.READ_ONLY)        // 읽기 전용
@Schema(accessMode = Schema.AccessMode.WRITE_ONLY)       // 쓰기 전용
@Schema(hidden = true)                                   // 숨김
```

### 응답 어노테이션
```java
@ApiResponse(responseCode = "200", description = "성공")
@ApiResponse(responseCode = "404", description = "없음",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
```

### 보안
```java
// 전역 JWT 설정
.components(new Components()
    .addSecuritySchemes("bearerAuth", new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")))

// 메서드별 적용
@SecurityRequirement(name = "bearerAuth")

// 보안 해제
@SecurityRequirements
```

### 환경별 on/off
```yaml
# 개발
springdoc.swagger-ui.enabled: true

# 운영
springdoc.swagger-ui.enabled: false
springdoc.api-docs.enabled: false
```

---

## 참고 자료

### 공식 문서
- [Springdoc OpenAPI](https://springdoc.org/)
- [Springdoc GitHub](https://github.com/springdoc/springdoc-openapi)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)

### 튜토리얼
- [Baeldung - Spring REST OpenAPI 3.0](https://www.baeldung.com/spring-rest-openapi-documentation)
- [Baeldung - JWT Authentication for OpenAPI](https://www.baeldung.com/openapi-jwt-authentication)

---

## 다음 단계

- [Spring Boot Docker 설정](SPRING_DOCKER_SETUP.md)
- [GitHub Actions CI/CD](GITHUB_ACTIONS_TUTORIAL.md)
- [PostgreSQL 튜토리얼](POSTGRESQL_TUTORIAL.md)
