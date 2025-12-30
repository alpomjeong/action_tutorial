# Docker vs CI/CD 비교 가이드

> Docker와 CI/CD의 차이점과 언제 무엇을 사용해야 하는지 알아보는 가이드

## 문서 정보

| 항목 | 내용 |
|------|------|
| **레벨** | 중급 |
| **예상 읽기 시간** | 15분 |
| **선행 지식** | Git 기초, 기본적인 개발 경험 |
| **최종 업데이트** | 2025년 1월 |

### 관련 문서
- [INDEX.md](INDEX.md) - 전체 문서 가이드
- [SPRING_DOCKER_SETUP.md](SPRING_DOCKER_SETUP.md) - Docker 환경 설정 상세
- [GITHUB_ACTIONS_TUTORIAL.md](GITHUB_ACTIONS_TUTORIAL.md) - GitHub Actions CI/CD 상세

---

## 목차

1. [Docker란?](#1-docker란)
2. [CI/CD란?](#2-cicd란)
3. [로컬 개발 환경 비교](#3-로컬-개발-환경-비교)
4. [CI/CD와 Docker의 관계](#4-cicd와-docker의-관계)
5. [실전 시나리오별 권장 구성](#5-실전-시나리오별-권장-구성)
6. [GitHub Actions에서 Docker 활용](#6-github-actions에서-docker-활용)
7. [결론](#7-결론)

---

## 개요

Docker와 CI/CD는 모두 "환경 일관성"과 관련이 있지만 목적이 다르다.

| | Docker | CI/CD |
|--|--------|-------|
| **핵심 목적** | 실행 환경 패키징 | 자동화된 검증/배포 |
| **해결하는 문제** | "어디서든 같은 환경" | "코드 변경마다 자동 검증" |
| **강제성** | 선택적 (안 써도 됨) | 강제적 (push하면 무조건 실행) |

---

## 1. Docker란?

### 개념

애플리케이션과 실행 환경을 하나의 패키지(이미지)로 묶는 기술.

```
┌─────────────────────────────┐
│  Docker Container           │
│  ┌───────────────────────┐  │
│  │  Application (Spring) │  │
│  │  Java 17              │  │
│  │  필요한 라이브러리들   │  │
│  └───────────────────────┘  │
└─────────────────────────────┘
```

### Dockerfile 예시

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose 예시

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: community
    ports:
      - "3306:3306"
```

### Docker의 장점

1. **환경 통일**: 모든 팀원이 동일한 환경에서 개발
2. **온보딩 간소화**: `docker-compose up` 한 줄로 환경 구성
3. **의존성 격리**: 프로젝트마다 다른 버전 사용 가능

### Docker의 단점

1. **리소스 소모**: 컨테이너 실행에 메모리/CPU 필요
2. **속도**: 네이티브 실행보다 느림
3. **학습 곡선**: Docker 자체를 배워야 함
4. **디버깅 복잡**: IDE 연동 설정 필요

---

## 2. CI/CD란?

### 개념

코드 변경 시 자동으로 빌드, 테스트, 배포를 수행하는 자동화 파이프라인.

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  Push   │ → │  Build  │ → │  Test   │ → │  Deploy │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
     │              │              │              │
     └──────────────┴──────────────┴──────────────┘
                    자동 실행
```

### CI (Continuous Integration)

코드 변경마다 자동으로 빌드/테스트.

```yaml
# .github/workflows/ci.yml
on:
  push:
    branches: [main, 'feature/**']

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: ./gradlew build
      - run: ./gradlew test
```

### CD (Continuous Deployment)

테스트 통과 후 자동으로 배포.

```yaml
# .github/workflows/cd.yml
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew build
      - name: Deploy to Server
        run: |
          scp build/libs/*.jar user@server:/app/
          ssh user@server "systemctl restart app"
```

### CI/CD의 장점

1. **강제 검증**: push하면 무조건 테스트 실행
2. **자동화**: 수동 작업 없이 빌드/배포
3. **빠른 피드백**: 문제 발생 시 즉시 알림
4. **일관된 환경**: CI 서버는 항상 동일한 환경

### CI/CD의 단점

1. **설정 필요**: 워크플로우 파일 작성
2. **실행 시간**: 빌드/테스트에 시간 소요
3. **비용**: 무료 시간 초과 시 과금 (GitHub Actions 월 2000분)

---

## 3. 로컬 개발 환경 비교

### 방식 A: 로컬 직접 설치

```
개발자 PC:
├── Java 17 (brew install openjdk@17)
├── MySQL 8.0 (brew install mysql)
├── Redis (brew install redis)
└── IDE (IntelliJ, VSCode)
```

**장점:**
- 빠른 실행 속도
- IDE 통합 쉬움
- 리소스 적게 사용

**단점:**
- 팀원마다 환경 다를 수 있음
- 새 팀원 온보딩 오래 걸림
- "내 컴에선 되는데" 문제

### 방식 B: Docker 개발 환경

```
개발자 PC:
├── Docker Desktop
└── docker-compose.yml
    ├── app (Spring Boot)
    ├── mysql (MySQL 8.0)
    └── redis (Redis)
```

**장점:**
- 모든 팀원 동일 환경
- 온보딩 간소화
- 프로젝트별 환경 격리

**단점:**
- Docker 리소스 소모
- 실행 속도 느림
- 디버깅 설정 복잡

### 방식 비교 표

| 기준 | 로컬 직접 설치 | Docker 개발환경 |
|------|---------------|-----------------|
| 설정 난이도 | 중간 (각자 설치) | 쉬움 (compose up) |
| 실행 속도 | 빠름 | 느림 |
| 메모리 사용 | 적음 | 많음 |
| 환경 통일 | 어려움 | 쉬움 |
| 온보딩 | "Java 깔고, MySQL 깔고..." | "Docker 설치 후 실행" |
| IDE 연동 | 쉬움 | 설정 필요 |
| 디버깅 | 쉬움 | 복잡 |

---

## 4. CI/CD와 Docker의 관계

### 케이스 1: CI/CD만 사용 (현재 프로젝트)

```
로컬: 각자 환경 (Java 직접 설치)
      ↓ push
CI:   ubuntu-latest + setup-java (통일된 환경)
      ↓ 테스트 통과
배포: JAR 파일 직접 배포
```

**적합한 경우:**
- 소규모 팀 (1-5명)
- 간단한 의존성 (H2, 내장 DB)
- 빠른 개발 속도 중시

### 케이스 2: Docker + CI/CD

```
로컬: Docker Compose (통일된 환경)
      ↓ push
CI:   Docker 이미지로 테스트 (로컬과 동일)
      ↓ 테스트 통과
배포: Docker 이미지 배포
```

**적합한 경우:**
- 대규모 팀 (10명 이상)
- 복잡한 의존성 (Kafka, Elasticsearch, 외부 API)
- 로컬-CI-배포 환경 100% 일치 필요

### 케이스 3: 로컬은 직접 설치, CI/CD에서 Docker

```
로컬: Java 직접 설치 (빠른 개발)
      ↓ push
CI:   Docker로 실제 DB 테스트
      ↓ 테스트 통과
배포: Docker 이미지 배포
```

**적합한 경우:**
- 중간 규모 팀
- 로컬 개발 속도 + 배포 환경 일치 둘 다 중요
- 가장 실용적인 조합

---

## 5. 실전 시나리오별 권장 구성

### 시나리오 1: 개인 프로젝트 / 학습

```
로컬: 직접 설치
CI:   GitHub Actions (기본)
배포: 없음 또는 수동
```

**이유:** 빠르고 간단, Docker 오버헤드 불필요

### 시나리오 2: 소규모 팀 (2-5명)

```
로컬: 직접 설치 + H2 인메모리 DB
CI:   GitHub Actions + 실제 DB (Docker services)
배포: Docker 이미지
```

```yaml
# CI에서만 Docker 사용
jobs:
  test:
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test
```

**이유:** 로컬 개발 속도 유지 + CI에서 실제 환경 검증

### 시나리오 3: 중규모 팀 (5-15명)

```
로컬: Docker Compose (선택적)
CI:   Docker 이미지
배포: Docker + Kubernetes
```

**이유:** 온보딩 비용 감소, 환경 통일 중요해짐

### 시나리오 4: 대규모 팀 / 마이크로서비스

```
로컬: Docker Compose (필수)
CI:   Docker 이미지 + 통합 테스트
배포: Kubernetes + ArgoCD
```

**이유:** 복잡한 의존성, 환경 일치 필수

---

## 6. GitHub Actions에서 Docker 활용

### 6.1 Services (사이드카 컨테이너)

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test
          MYSQL_DATABASE: community
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3

      redis:
        image: redis:7
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4
      - name: Test with real DB
        run: ./gradlew test
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/community
```

### 6.2 Container (전체 Job을 컨테이너에서 실행)

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: openjdk:17-jdk-slim
      options: --user root

    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew test
```

### 6.3 커스텀 Docker 이미지 빌드 & 푸시

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: myuser/myapp:${{ github.sha }}
```

---

## 7. 결론

### CI/CD가 잘 되어 있으면 로컬에서 Docker 안 써도 된다?

**YES**, 대부분의 경우 가능하다.

```
CI/CD의 역할:
├── 통일된 환경에서 검증 (환경 차이 문제 해결)
├── 강제 테스트 (품질 보장)
└── 자동 배포 (일관된 배포)

Docker의 역할:
├── 로컬 개발 환경 통일 (선택적)
├── 복잡한 의존성 관리 (필요할 때)
└── 배포 환경 패키징 (권장)
```

### 권장 사항

| 상황 | 로컬 환경 | CI | 배포 |
|------|----------|-----|------|
| 개인/학습 | 직접 설치 | GitHub Actions | 수동 |
| 소규모 팀 | 직접 설치 | GA + Docker Services | Docker |
| 중규모 팀 | Docker 선택적 | Docker | Docker/K8s |
| 대규모 팀 | Docker 필수 | Docker | K8s |

### 핵심 정리

```
Docker: "환경을 같게 만드는 도구" (선택적)
CI/CD:  "검증을 강제하는 시스템" (필수)

→ CI/CD 없이 Docker만 있으면?
  테스트 안 돌리고 push 가능 (품질 보장 X)

→ Docker 없이 CI/CD만 있으면?
  로컬 환경 달라도 CI에서 검증됨 (품질 보장 O)

→ 둘 다 있으면?
  최고의 조합 (환경 통일 + 품질 보장)
```

---

## 의사결정 플로우차트

```
                    ┌─────────────────┐
                    │  프로젝트 시작   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ 팀 규모가 5명    │
                    │    이상인가?     │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │ No           │              │ Yes
              ▼              │              ▼
    ┌─────────────────┐      │    ┌─────────────────┐
    │ 로컬: 직접 설치  │      │    │ 로컬: Docker    │
    │ CI: GA 기본     │      │    │ CI: Docker      │
    └─────────────────┘      │    └─────────────────┘
                             │
                    ┌────────▼────────┐
                    │  복잡한 의존성   │
                    │   있는가?        │
                    │ (Kafka, ES 등)  │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │ No           │              │ Yes
              ▼              │              ▼
    ┌─────────────────┐      │    ┌─────────────────┐
    │ Docker 선택적   │      │    │ Docker 필수     │
    └─────────────────┘      │    └─────────────────┘
```

---

## 다음 단계

- [Docker 환경 상세 설정](SPRING_DOCKER_SETUP.md)
- [GitHub Actions CI/CD 구성](GITHUB_ACTIONS_TUTORIAL.md)
- [데이터베이스 서비스 선택](DATABASE_SERVICE_COMPARISON.md)
