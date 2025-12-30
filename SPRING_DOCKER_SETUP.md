# Spring Boot + Supabase Docker 환경 세팅 가이드

## 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot (백엔드)                  │
│                    - REST API                           │
│                    - 비즈니스 로직                       │
└─────────────────────┬───────────────────────────────────┘
                      │ JDBC (PostgreSQL)
                      ▼
┌─────────────────────────────────────────────────────────┐
│                    Supabase                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ PostgreSQL  │  │    Auth     │  │   Storage   │     │
│  │   (DB)      │  │   (인증)    │  │  (파일)     │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
```

**Spring Boot**: 비즈니스 로직, API 개발
**Supabase**: PostgreSQL DB 호스팅 (+ 인증, 스토리지 옵션)

---

## 버전 기준 (2025년 현업 표준)

| 항목 | 버전 | 비고 |
|------|------|------|
| Spring Boot | 3.2.x | 최신 안정 |
| Java | 17 (LTS) | Temurin 권장 |
| Gradle | 8.5+ | Spring Boot 3.2 요구 |
| PostgreSQL | 15.x | Supabase 기본 |
| Redis | 7.x | 캐시용 (선택) |

---

## 1. Supabase 설정

### 1.1 Supabase 프로젝트 생성

1. https://supabase.com 접속
2. New Project 생성
3. 프로젝트 Settings → Database에서 연결 정보 확인:

```
Host:     db.<project-ref>.supabase.co
Port:     5432
Database: postgres
User:     postgres
Password: [프로젝트 생성 시 설정한 비밀번호]
```

### 1.2 Connection String

```
# Direct Connection (권장)
jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres

# Connection Pooler (동시 연결 많을 때)
jdbc:postgresql://db.<project-ref>.supabase.co:6543/postgres?pgbouncer=true
```

---

## 2. build.gradle 설정

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // PostgreSQL (Supabase)
    runtimeOnly 'org.postgresql:postgresql'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'com.h2database:h2'  // 테스트용 인메모리 DB
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## 3. Dockerfile

### 3.1 멀티스테이지 빌드 (권장)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 먼저 다운로드 (캐싱 활용)
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew build -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 빌드된 JAR만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 보안: non-root 사용자
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. Docker Compose

### 4.1 로컬 개발 - Supabase 직접 연결

Supabase 클라우드에 직접 연결 (DB 설치 불필요).

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SUPABASE_URL=${SUPABASE_URL}
      - SUPABASE_DB_HOST=${SUPABASE_DB_HOST}
      - SUPABASE_DB_PASSWORD=${SUPABASE_DB_PASSWORD}
    env_file:
      - .env
```

**.env 파일 (gitignore에 추가)**:
```env
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_DB_HOST=db.<project-ref>.supabase.co
SUPABASE_DB_PASSWORD=your-database-password
```

### 4.2 로컬 개발 - PostgreSQL Docker (오프라인 개발)

Supabase 없이 로컬 PostgreSQL로 개발.

```yaml
# docker-compose.local.yml
version: '3.8'

services:
  # 로컬 PostgreSQL (Supabase 대체)
  postgres:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: community
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis (캐시, 선택사항)
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  postgres-data:
```

### 4.3 운영 환경 (Supabase 클라우드)

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  app:
    image: myregistry/myapp:${TAG:-latest}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SUPABASE_DB_HOST=${SUPABASE_DB_HOST}
      - SUPABASE_DB_PASSWORD=${SUPABASE_DB_PASSWORD}
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1'
          memory: 1G
    restart: always
```

---

## 5. 환경별 application.yml

### 5.1 공통 설정 (application.yml)

```yaml
spring:
  application:
    name: community

server:
  port: 8080

# JPA 공통 설정
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### 5.2 로컬 개발 - Supabase 연결 (application-dev.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${SUPABASE_DB_HOST:localhost}:5432/postgres
    username: postgres
    password: ${SUPABASE_DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    root: INFO
    com.example: DEBUG
    org.hibernate.SQL: DEBUG
```

### 5.3 로컬 개발 - 로컬 PostgreSQL (application-local.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/community
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

logging:
  level:
    root: INFO
    com.example: DEBUG
```

### 5.4 테스트 환경 (application-test.yml)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
```

### 5.5 운영 환경 (application-prod.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${SUPABASE_DB_HOST}:5432/postgres
    username: postgres
    password: ${SUPABASE_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate  # 운영에서는 validate만
    show-sql: false

logging:
  level:
    root: WARN
    com.example: INFO
```

---

## 6. 개발 시나리오별 실행 방법

### 시나리오 1: Supabase 직접 연결 (권장)

```bash
# .env 파일 설정 후
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 시나리오 2: 로컬 PostgreSQL로 개발 (오프라인)

```bash
# PostgreSQL 컨테이너 실행
docker-compose -f docker-compose.local.yml up -d postgres

# Spring 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 시나리오 3: 테스트 실행 (H2 인메모리)

```bash
./gradlew test
# application-test.yml의 H2 사용
```

### 시나리오 4: Docker로 전체 실행

```bash
# 빌드
./gradlew build -x test

# Docker 실행
docker-compose up --build
```

---

## 7. GitHub Actions

### 7.1 CI (H2로 테스트)

```yaml
name: CI

on:
  push:
    branches: [main, 'feature/**']
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission
        run: chmod +x gradlew

      - name: Test
        run: ./gradlew test
        env:
          SPRING_PROFILES_ACTIVE: test
```

### 7.2 CI (PostgreSQL로 통합 테스트)

```yaml
name: Integration Test

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: community
        ports:
          - 5432:5432
        options: >-
          --health-cmd="pg_isready -U postgres"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission
        run: chmod +x gradlew

      - name: Integration Test
        run: ./gradlew test
        env:
          SPRING_PROFILES_ACTIVE: local
```

### 7.3 Docker 이미지 빌드 & 푸시

```yaml
name: Build and Push Docker Image

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Build JAR
        run: ./gradlew build -x test

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/community-app:latest
            ${{ secrets.DOCKER_USERNAME }}/community-app:${{ github.sha }}
```

---

## 8. .env 템플릿

### .env.example (저장소에 포함)

```env
# Supabase 설정
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_DB_HOST=db.your-project.supabase.co
SUPABASE_DB_PASSWORD=your-password

# 옵션: Supabase API Key (인증 사용 시)
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_KEY=your-service-key
```

### .gitignore에 추가

```gitignore
# Environment
.env
.env.local
.env.*.local
```

---

## 9. 프로젝트 구조

```
project/
├── .github/
│   └── workflows/
│       ├── ci.yml                  # CI (테스트)
│       └── docker-build.yml        # Docker 빌드
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── application.yml      # 공통
│   │       ├── application-dev.yml  # Supabase 연결
│   │       ├── application-local.yml # 로컬 PostgreSQL
│   │       ├── application-test.yml  # H2 테스트
│   │       └── application-prod.yml  # 운영
│   └── test/
├── gradle/
│   └── wrapper/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── Dockerfile
├── docker-compose.yml              # Supabase 연결
├── docker-compose.local.yml        # 로컬 PostgreSQL
├── docker-compose.prod.yml         # 운영
├── .dockerignore
├── .gitignore
├── .env.example                    # 환경변수 템플릿
└── .env                            # 실제 환경변수 (gitignore)
```

---

## 10. Supabase vs 로컬 PostgreSQL 비교

| 기준 | Supabase 직접 연결 | 로컬 PostgreSQL |
|------|-------------------|-----------------|
| 설정 | .env만 설정 | Docker 필요 |
| 오프라인 개발 | 불가 | 가능 |
| 데이터 공유 | 팀원과 공유 | 각자 별도 |
| 비용 | 무료 티어 있음 | 무료 |
| 운영 환경 동일성 | 높음 | 중간 |

### 권장 조합

```
개발:   Supabase 직접 연결 (온라인) 또는 로컬 PostgreSQL (오프라인)
테스트: H2 인메모리 (빠름)
CI:     PostgreSQL Docker (통합 테스트)
운영:   Supabase 클라우드
```

---

## 11. 체크리스트

### Supabase 설정
- [ ] Supabase 프로젝트 생성
- [ ] 데이터베이스 비밀번호 설정
- [ ] .env 파일 작성
- [ ] .gitignore에 .env 추가

### 프로젝트 설정
- [ ] build.gradle에 PostgreSQL 의존성
- [ ] 환경별 application-{profile}.yml 작성
- [ ] Dockerfile 작성

### CI/CD
- [ ] GitHub Actions 워크플로우
- [ ] Docker 이미지 빌드

---

## 12. 빠른 시작

```bash
# 1. Supabase 프로젝트 생성 후 .env 작성
cp .env.example .env
# .env 파일 편집

# 2. Spring 실행 (Supabase 연결)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 로컬 PostgreSQL로 개발
docker-compose -f docker-compose.local.yml up -d postgres
./gradlew bootRun --args='--spring.profiles.active=local'
```
