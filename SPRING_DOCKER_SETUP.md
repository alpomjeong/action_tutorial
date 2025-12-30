# Spring Boot + AWS RDS PostgreSQL Docker 환경 세팅 가이드

> Spring Boot 프로젝트를 Docker와 AWS RDS PostgreSQL로 구성하는 실전 가이드

## 문서 정보

| 항목 | 내용 |
|------|------|
| **레벨** | 초급 ~ 중급 |
| **예상 읽기 시간** | 25분 |
| **선행 지식** | Spring Boot 기초, Docker 기본 개념, AWS 기초 |
| **최종 업데이트** | 2025년 1월 |

### 관련 문서
- [INDEX.md](INDEX.md) - 전체 문서 가이드
- [DATABASE_SERVICE_COMPARISON.md](DATABASE_SERVICE_COMPARISON.md) - DB 서비스 비교
- [GITHUB_ACTIONS_TUTORIAL.md](GITHUB_ACTIONS_TUTORIAL.md) - CI/CD 설정
- [DOCKER_VS_CI_COMPARISON.md](DOCKER_VS_CI_COMPARISON.md) - Docker vs CI 비교

---

## 목차

1. [AWS RDS 설정](#1-aws-rds-설정)
2. [build.gradle 설정](#2-buildgradle-설정)
3. [Dockerfile](#3-dockerfile)
4. [Docker Compose](#4-docker-compose)
5. [환경별 application.yml](#5-환경별-applicationyml)
6. [개발 시나리오별 실행 방법](#6-개발-시나리오별-실행-방법)
7. [GitHub Actions](#7-github-actions)
8. [.env 템플릿](#8-env-템플릿)
9. [프로젝트 구조](#9-프로젝트-구조)
10. [AWS RDS vs 로컬 PostgreSQL 비교](#10-aws-rds-vs-로컬-postgresql-비교)
11. [체크리스트](#11-체크리스트)
12. [빠른 시작](#12-빠른-시작)
13. [문제 해결](#13-문제-해결)
14. [보안 설정](#14-보안-설정)

---

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
│                      AWS                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  RDS        │  │    EC2      │  │     S3      │     │
│  │ PostgreSQL  │  │  (Optional) │  │  (Storage)  │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
```

**Spring Boot**: 비즈니스 로직, API 개발
**AWS RDS**: PostgreSQL 관리형 데이터베이스

---

## 버전 기준 (2025년 현업 표준)

| 항목 | 버전 | 비고 |
|------|------|------|
| Spring Boot | 3.2.x | 최신 안정 |
| Java | 17 (LTS) | Temurin 권장 |
| Gradle | 8.5+ | Spring Boot 3.2 요구 |
| PostgreSQL | 15.x | AWS RDS 지원 |
| Redis | 7.x | ElastiCache (선택) |

---

## 1. AWS RDS 설정

### 1.1 RDS 인스턴스 생성

**AWS Console:**
1. AWS Console → RDS → Create database
2. 설정:

| 항목 | 값 |
|------|-----|
| Engine | PostgreSQL 15.x |
| Template | Free tier (개발) / Production (운영) |
| DB instance identifier | community-db |
| Master username | postgres |
| Master password | [강력한 비밀번호] |
| DB instance class | db.t3.micro (개발) / db.t3.small+ (운영) |
| Storage | 20GB (gp2) |
| Public access | Yes (개발) / No (운영) |

**AWS CLI:**
```bash
aws rds create-db-instance \
  --db-instance-identifier community-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username postgres \
  --master-user-password YOUR_PASSWORD \
  --allocated-storage 20 \
  --publicly-accessible
```

### 1.2 보안 그룹 설정

```
인바운드 규칙:
┌──────────┬──────────┬─────────────────┐
│ Type     │ Port     │ Source          │
├──────────┼──────────┼─────────────────┤
│ PostgreSQL│ 5432    │ 내 IP           │  ← 개발용
│ PostgreSQL│ 5432    │ EC2 보안그룹    │  ← 운영용
└──────────┴──────────┴─────────────────┘
```

### 1.3 연결 정보 확인

RDS 콘솔에서 엔드포인트 확인:

```
Host:     community-db.xxxx.ap-northeast-2.rds.amazonaws.com
Port:     5432
Database: postgres
User:     postgres
Password: [설정한 비밀번호]
```

### 1.4 Connection String

```
jdbc:postgresql://community-db.xxxx.ap-northeast-2.rds.amazonaws.com:5432/postgres
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
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // PostgreSQL
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

# JVM 최적화 옵션
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

---

## 4. Docker Compose

### 4.1 로컬 개발 - AWS RDS 직접 연결

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
      - DB_HOST=${DB_HOST}
      - DB_PORT=${DB_PORT}
      - DB_NAME=${DB_NAME}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
    env_file:
      - .env
```

### 4.2 로컬 개발 - PostgreSQL Docker (오프라인 개발)

```yaml
# docker-compose.local.yml
version: '3.8'

services:
  # 로컬 PostgreSQL (AWS RDS 대체)
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

  # Spring Boot 앱
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres-data:
```

### 4.3 운영 환경 (AWS RDS 연결)

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  app:
    image: ${ECR_REGISTRY}/community-app:${TAG:-latest}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=${DB_HOST}
      - DB_PORT=5432
      - DB_NAME=${DB_NAME}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1'
          memory: 1G
    restart: always
    logging:
      driver: awslogs
      options:
        awslogs-group: /ecs/community-app
        awslogs-region: ap-northeast-2
        awslogs-stream-prefix: app
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
    open-in-view: false
```

### 5.2 개발 환경 - AWS RDS 연결 (application-dev.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME:postgres}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
      connection-timeout: 30000

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
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

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

### 시나리오 1: AWS RDS 직접 연결 (권장)

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

### 시나리오 4: Docker로 전체 실행 (로컬)

```bash
# 빌드
./gradlew build -x test

# 로컬 전체 스택 실행
docker-compose -f docker-compose.local.yml up --build
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

### 7.3 Docker 이미지 빌드 & ECR 푸시

```yaml
name: Build and Push to ECR

on:
  push:
    branches: [main]
    tags: ['v*']

env:
  AWS_REGION: ap-northeast-2
  ECR_REPOSITORY: community-app

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

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build and Push Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:latest .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
```

---

## 8. .env 템플릿

### .env.example (저장소에 포함)

```env
# AWS RDS 설정
DB_HOST=community-db.xxxx.ap-northeast-2.rds.amazonaws.com
DB_PORT=5432
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=your-secure-password

# AWS 설정 (선택)
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key

# ECR 설정 (선택)
ECR_REGISTRY=123456789.dkr.ecr.ap-northeast-2.amazonaws.com
```

### .gitignore에 추가

```gitignore
# Environment
.env
.env.local
.env.*.local

# AWS
.aws/
```

---

## 9. 프로젝트 구조

```
project/
├── .github/
│   └── workflows/
│       ├── ci.yml                  # CI (테스트)
│       ├── integration-test.yml    # 통합 테스트
│       └── ecr-push.yml            # ECR 푸시
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── application.yml      # 공통
│   │       ├── application-dev.yml  # AWS RDS 연결
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
├── docker-compose.yml              # AWS RDS 연결
├── docker-compose.local.yml        # 로컬 PostgreSQL
├── docker-compose.prod.yml         # 운영
├── .dockerignore
├── .gitignore
├── .env.example                    # 환경변수 템플릿
└── .env                            # 실제 환경변수 (gitignore)
```

---

## 10. AWS RDS vs 로컬 PostgreSQL 비교

| 기준 | AWS RDS | 로컬 PostgreSQL |
|------|---------|-----------------|
| 설정 | AWS 콘솔에서 생성 | Docker 필요 |
| 오프라인 개발 | 불가 | 가능 |
| 데이터 공유 | 팀원과 공유 가능 | 각자 별도 |
| 비용 | 유료 (프리티어 있음) | 무료 |
| 운영 환경 동일성 | 높음 | 중간 |
| 백업/복구 | 자동 | 수동 |
| 확장성 | 쉬움 | 어려움 |

### 권장 조합

```
개발:   AWS RDS 직접 연결 (온라인) 또는 로컬 PostgreSQL (오프라인)
테스트: H2 인메모리 (빠름)
CI:     PostgreSQL Docker (통합 테스트)
운영:   AWS RDS (Multi-AZ 권장)
```

---

## 11. 체크리스트

### AWS 설정
- [ ] AWS 계정 생성
- [ ] RDS 인스턴스 생성
- [ ] 보안 그룹 설정 (5432 포트)
- [ ] IAM 사용자 생성 (ECR 푸시용)
- [ ] ECR 저장소 생성

### 프로젝트 설정
- [ ] build.gradle에 PostgreSQL 의존성
- [ ] 환경별 application-{profile}.yml 작성
- [ ] Dockerfile 작성
- [ ] .env 파일 작성

### CI/CD
- [ ] GitHub Secrets 설정 (AWS 키, DB 비밀번호)
- [ ] GitHub Actions 워크플로우
- [ ] ECR 푸시 확인

---

## 12. 빠른 시작

```bash
# 1. AWS RDS 생성 후 .env 작성
cp .env.example .env
# .env 파일에 RDS 엔드포인트와 비밀번호 입력

# 2. Spring 실행 (AWS RDS 연결)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 로컬 PostgreSQL로 개발
docker-compose -f docker-compose.local.yml up -d postgres
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## 13. 문제 해결

### 자주 발생하는 오류

#### 1. RDS 연결 실패
```
Connection refused to host: xxx.rds.amazonaws.com
```

**해결:**
- RDS 인스턴스가 running 상태인지 확인
- 보안 그룹에서 5432 포트 허용 확인
- Public access 설정 확인 (개발 환경)
- .env 파일의 호스트 주소 확인

#### 2. 인증 실패
```
FATAL: password authentication failed for user "postgres"
```

**해결:**
- RDS Master password 확인
- .env 파일의 비밀번호 확인
- 특수문자 이스케이프 필요할 수 있음

#### 3. Gradle 빌드 실패
```
Could not resolve org.springframework.boot:spring-boot-starter-web
```

**해결:**
```bash
# Gradle 캐시 삭제 후 재시도
./gradlew clean build --refresh-dependencies
```

#### 4. Docker 빌드 시 JAR 파일 없음
```
COPY failed: no source files were specified
```

**해결:**
```bash
# Docker 빌드 전 JAR 먼저 생성
./gradlew build -x test
docker-compose up --build
```

#### 5. 포트 충돌
```
Bind for 0.0.0.0:5432 failed: port is already allocated
```

**해결:**
```bash
# 기존 PostgreSQL 컨테이너 중지
docker stop $(docker ps -q --filter ancestor=postgres)

# 또는 다른 포트 사용
ports:
  - "5433:5432"  # 호스트 포트 변경
```

### 디버깅 팁

```bash
# 1. RDS 연결 테스트
psql "postgresql://postgres:PASSWORD@xxx.rds.amazonaws.com:5432/postgres"

# 2. Docker 컨테이너 로그 확인
docker-compose logs -f app

# 3. Spring Boot 상세 로그
./gradlew bootRun --args='--spring.profiles.active=dev --debug'

# 4. 환경변수 확인
docker-compose config

# 5. RDS 상태 확인 (AWS CLI)
aws rds describe-db-instances --db-instance-identifier community-db
```

---

## 14. 보안 설정

### 운영 환경 보안 권장사항

#### 1. RDS 보안
```
- Public access: No (운영)
- VPC 내부에서만 접근
- 보안 그룹으로 EC2/ECS에서만 접근 허용
- SSL 연결 강제
```

#### 2. SSL 연결 설정
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}?sslmode=require
```

#### 3. IAM 인증 (선택)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: # IAM 토큰 사용
```

#### 4. Secrets Manager 사용 (권장)
```yaml
# AWS Secrets Manager에서 자동으로 비밀번호 가져오기
spring:
  cloud:
    aws:
      secretsmanager:
        enabled: true
```

### 비밀번호 정책
```
- 최소 16자 이상
- 대문자, 소문자, 숫자, 특수문자 포함
- 정기적 교체 (90일)
- Secrets Manager로 관리 권장
```

---

## 다음 단계

- [GitHub Actions로 CI/CD 구성하기](GITHUB_ACTIONS_TUTORIAL.md)
- [Docker vs CI/CD 전략 비교](DOCKER_VS_CI_COMPARISON.md)
- [테스트 모듈화 전략](SPRING_TEST_MODULARIZATION.md)
- [데이터베이스 서비스 비교](DATABASE_SERVICE_COMPARISON.md)
