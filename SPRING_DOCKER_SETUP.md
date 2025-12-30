# Spring Boot Docker 환경 세팅 가이드

## 버전 기준 (2025년 현업 표준)

| 항목 | 버전 | 비고 |
|------|------|------|
| Spring Boot | 3.2.x | 최신 안정 |
| Java | 17 (LTS) | Temurin 권장 |
| Gradle | 8.5+ | Spring Boot 3.2 요구 |
| MySQL | 8.0 | LTS |
| Redis | 7.x | 최신 안정 |

---

## 1. Dockerfile

### 1.1 기본 Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 1.2 멀티스테이지 빌드 (권장)

빌드와 실행 환경을 분리하여 이미지 크기 최소화.

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

### 1.3 이미지 크기 비교

| 방식 | 이미지 크기 |
|------|------------|
| 기본 (JDK 포함) | ~400MB |
| 멀티스테이지 (JRE만) | ~200MB |
| Alpine + JRE | ~150MB |

---

## 2. Docker Compose

### 2.1 개발 환경 (docker-compose.yml)

```yaml
version: '3.8'

services:
  # Spring Boot 애플리케이션
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/community
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root
      - SPRING_REDIS_HOST=redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - app-network

  # MySQL 8.0
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: community
      TZ: Asia/Seoul
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql  # 초기화 SQL
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  # Redis 7
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - app-network

volumes:
  mysql-data:
  redis-data:

networks:
  app-network:
    driver: bridge
```

### 2.2 개발용 (로컬 코드 마운트)

```yaml
version: '3.8'

services:
  # 로컬 개발 시 DB만 Docker로
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: community
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  mysql-data:
```

### 2.3 운영 환경 (docker-compose.prod.yml)

```yaml
version: '3.8'

services:
  app:
    image: myregistry/myapp:${TAG:-latest}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=${DATABASE_URL}
      - SPRING_DATASOURCE_USERNAME=${DATABASE_USER}
      - SPRING_DATASOURCE_PASSWORD=${DATABASE_PASSWORD}
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1'
          memory: 1G
    restart: always
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

---

## 3. 환경별 application.yml

### 3.1 공통 설정 (application.yml)

```yaml
spring:
  application:
    name: community

server:
  port: 8080
```

### 3.2 개발 환경 (application-dev.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/community
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  redis:
    host: localhost
    port: 6379

logging:
  level:
    root: INFO
    com.example: DEBUG
    org.hibernate.SQL: DEBUG
```

### 3.3 운영 환경 (application-prod.yml)

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate  # 운영에서는 validate만
    show-sql: false

  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}

logging:
  level:
    root: WARN
    com.example: INFO
```

---

## 4. Docker 명령어 정리

### 4.1 개발 환경 실행

```bash
# DB만 Docker로 (Spring은 로컬에서 실행)
docker-compose up -d mysql redis

# Spring 로컬 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 4.2 전체 Docker로 실행

```bash
# 빌드
./gradlew build -x test

# Docker 이미지 빌드 및 실행
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build
```

### 4.3 운영 환경 실행

```bash
# 환경변수 설정
export TAG=v1.0.0
export DATABASE_URL=jdbc:mysql://prod-db:3306/community
export DATABASE_USER=produser
export DATABASE_PASSWORD=securepassword

# 실행
docker-compose -f docker-compose.prod.yml up -d
```

### 4.4 유용한 명령어

```bash
# 로그 확인
docker-compose logs -f app

# 컨테이너 접속
docker-compose exec app sh

# MySQL 접속
docker-compose exec mysql mysql -u root -p

# 전체 종료 및 볼륨 삭제
docker-compose down -v

# 이미지 재빌드 (캐시 무시)
docker-compose build --no-cache
```

---

## 5. GitHub Actions + Docker

### 5.1 CI에서 Docker Services 사용

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

    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: community
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

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

      - name: Test with real DB
        run: ./gradlew test
        env:
          SPRING_PROFILES_ACTIVE: dev
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/community
          SPRING_DATASOURCE_USERNAME: root
          SPRING_DATASOURCE_PASSWORD: root
          SPRING_REDIS_HOST: localhost
```

### 5.2 Docker 이미지 빌드 & 푸시

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

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ secrets.DOCKER_USERNAME }}/community-app
          tags: |
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=sha,prefix=

      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

---

## 6. .dockerignore

```dockerignore
# Git
.git
.gitignore

# Gradle
.gradle
build
!build/libs/*.jar

# IDE
.idea
*.iml
.vscode

# Logs
*.log
logs/

# Test
src/test

# Docker
Dockerfile*
docker-compose*

# Docs
*.md
```

---

## 7. 프로젝트 파일 구조

```
project/
├── .github/
│   └── workflows/
│       ├── ci.yml              # CI 워크플로우
│       └── docker-build.yml    # Docker 빌드
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
├── gradle/
│   └── wrapper/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── Dockerfile                  # 멀티스테이지 빌드
├── docker-compose.yml          # 개발 환경
├── docker-compose.prod.yml     # 운영 환경
├── .dockerignore
├── .gitignore
└── init.sql                    # DB 초기화 (선택)
```

---

## 8. 체크리스트

### 개발 환경 구성

- [ ] Dockerfile 작성 (멀티스테이지)
- [ ] docker-compose.yml 작성
- [ ] .dockerignore 작성
- [ ] 환경별 application-{profile}.yml 분리

### CI/CD 구성

- [ ] GitHub Actions CI 워크플로우
- [ ] Docker 이미지 빌드 워크플로우
- [ ] Docker Hub 또는 Registry 연동

### 보안

- [ ] Dockerfile에서 non-root 사용자 사용
- [ ] 민감 정보는 환경변수로 관리
- [ ] .dockerignore에 민감 파일 제외

---

## 9. 빠른 시작

```bash
# 1. DB 컨테이너 실행
docker-compose up -d mysql redis

# 2. 로컬에서 Spring 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 전체 Docker로 실행
docker-compose up --build
```
