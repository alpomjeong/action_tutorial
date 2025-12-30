# Spring Boot 프로젝트 문서 가이드

> 이 저장소의 문서들을 효과적으로 활용하기 위한 가이드입니다.

---

## 문서 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    1. 데이터베이스 선택                      │
│                 DATABASE_SERVICE_COMPARISON.md              │
│            (Supabase vs AWS vs GCP 비교)                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  2. PostgreSQL 입문                         │
│                   POSTGRESQL_TUTORIAL.md                    │
│           (MySQL 개발자를 위한 PostgreSQL 가이드)            │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  3. 로컬 개발 환경 구성                      │
│                  SPRING_DOCKER_SETUP.md                     │
│           (Docker + Spring Boot + AWS RDS)                  │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   4. CI/CD 파이프라인                        │
│                GITHUB_ACTIONS_TUTORIAL.md                   │
│              (GitHub Actions 워크플로우)                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    5. 배포 전략 선택                         │
│                DOCKER_VS_CI_COMPARISON.md                   │
│              (Docker vs CI/CD 비교 분석)                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    6. 테스트 전략                            │
│              SPRING_TEST_MODULARIZATION.md                  │
│             (테스트 모듈화 및 자동화)                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   7. API 문서화                              │
│                   SWAGGER_TUTORIAL.md                       │
│             (Swagger/OpenAPI 3.0 적용)                      │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 8. 아키텍처 설계                             │
│              CLEAN_ARCHITECTURE_TUTORIAL.md                 │
│            (Clean Architecture 적용 가이드)                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 9. 알림 시스템 구현                          │
│           NOTIFICATION_ARCHITECTURE_TUTORIAL.md             │
│          (비동기 알림 + 이벤트 기반 아키텍처)                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 상황별 가이드

### 처음 프로젝트에 합류했다면?

```
1. DATABASE_SERVICE_COMPARISON.md → 용어집 먼저 읽기
2. POSTGRESQL_TUTORIAL.md → PostgreSQL 기초 학습 (MySQL 경험자)
3. SPRING_DOCKER_SETUP.md → 빠른 시작 섹션으로 환경 구성
4. GITHUB_ACTIONS_TUTORIAL.md → CI 동작 방식 이해
```

### 데이터베이스를 선택해야 한다면?

| 상황 | 추천 문서 | 참고 섹션 |
|------|----------|----------|
| Supabase vs AWS 고민 | DATABASE_SERVICE_COMPARISON.md | 섹션 1-4 |
| MySQL vs PostgreSQL 고민 | DATABASE_SERVICE_COMPARISON.md | 섹션 10 |
| 비용 계산 필요 | DATABASE_SERVICE_COMPARISON.md | 섹션 6 |
| PostgreSQL 처음 배우기 | POSTGRESQL_TUTORIAL.md | 섹션 1-3 |
| PostgreSQL 고급 기능 | POSTGRESQL_TUTORIAL.md | 섹션 4 |
| MySQL → PostgreSQL 마이그레이션 | POSTGRESQL_TUTORIAL.md | 섹션 6 |

### 개발 환경을 구성해야 한다면?

| 상황 | 추천 문서 | 참고 섹션 |
|------|----------|----------|
| Docker로 로컬 환경 구성 | SPRING_DOCKER_SETUP.md | 섹션 4 |
| AWS RDS 연결 설정 | SPRING_DOCKER_SETUP.md | 섹션 1, 5 |
| 환경별 설정 분리 | SPRING_DOCKER_SETUP.md | 섹션 5 |
| JPA/Hibernate PostgreSQL 설정 | POSTGRESQL_TUTORIAL.md | 섹션 9 |

### CI/CD를 구성해야 한다면?

| 상황 | 추천 문서 | 참고 섹션 |
|------|----------|----------|
| GitHub Actions 처음 | GITHUB_ACTIONS_TUTORIAL.md | 섹션 1-5 |
| Docker vs CI 차이점 | DOCKER_VS_CI_COMPARISON.md | 섹션 1-3 |
| 팀 규모별 전략 | DOCKER_VS_CI_COMPARISON.md | 섹션 5 |

### 테스트를 효율화하고 싶다면?

| 상황 | 추천 문서 | 참고 섹션 |
|------|----------|----------|
| 테스트 중복 제거 | SPRING_TEST_MODULARIZATION.md | 섹션 3 |
| 테스트 패턴 비교 | SPRING_TEST_MODULARIZATION.md | 섹션 5 |

### API 문서화가 필요하다면?

| 상황 | 추천 문서 | 참고 섹션 |
|------|----------|----------|
| Swagger 기본 설정 | SWAGGER_TUTORIAL.md | 섹션 1-3 |
| 컨트롤러 어노테이션 | SWAGGER_TUTORIAL.md | 섹션 4-6 |
| JWT 인증 설정 | SWAGGER_TUTORIAL.md | 섹션 7 |
| API 그룹화 | SWAGGER_TUTORIAL.md | 섹션 8 |
| 운영 환경 비활성화 | SWAGGER_TUTORIAL.md | 섹션 9 |

### 아키텍처를 개선하고 싶다면?

| 상황 | 추천 문서 | 참고 섹션 |
|------|----------|----------|
| Clean Architecture 개념 | CLEAN_ARCHITECTURE_TUTORIAL.md | 섹션 1-2 |
| 4개 레이어 이해 | CLEAN_ARCHITECTURE_TUTORIAL.md | 섹션 3 |
| Spring Boot 프로젝트 구조 | CLEAN_ARCHITECTURE_TUTORIAL.md | 섹션 5 |
| 실전 구현 예제 | CLEAN_ARCHITECTURE_TUTORIAL.md | 섹션 6 |
| Port/Adapter 패턴 | CLEAN_ARCHITECTURE_TUTORIAL.md | 섹션 7 |
| 아키텍처 테스트 전략 | CLEAN_ARCHITECTURE_TUTORIAL.md | 섹션 8 |
| 점진적 도입 방법 | CLEAN_ARCHITECTURE_TUTORIAL.md | 섹션 10 |

### 알림 기능을 구현하고 싶다면?

| 상황 | 추천 문서 | 참고 섹션 |
|------|----------|----------|
| 알림 시스템 개요 | NOTIFICATION_ARCHITECTURE_TUTORIAL.md | 섹션 1 |
| 알림 도메인 설계 | NOTIFICATION_ARCHITECTURE_TUTORIAL.md | 섹션 3 |
| 비동기 처리 (@Async) | NOTIFICATION_ARCHITECTURE_TUTORIAL.md | 섹션 7 |
| 이벤트 기반 알림 발송 | NOTIFICATION_ARCHITECTURE_TUTORIAL.md | 섹션 6 |
| FCM 푸시 알림 | NOTIFICATION_ARCHITECTURE_TUTORIAL.md | 섹션 5 |
| 알림 확장 전략 | NOTIFICATION_ARCHITECTURE_TUTORIAL.md | 섹션 10 |

---

## 문서 목록

| 문서 | 설명 | 레벨 | 읽기 시간 |
|------|------|------|----------|
| [DATABASE_SERVICE_COMPARISON.md](DATABASE_SERVICE_COMPARISON.md) | DB 서비스 비교 (Supabase, AWS, GCP) | 초급 | 25분 |
| [POSTGRESQL_TUTORIAL.md](POSTGRESQL_TUTORIAL.md) | PostgreSQL 입문 (MySQL 비교) | 초급~중급 | 40분 |
| [SPRING_DOCKER_SETUP.md](SPRING_DOCKER_SETUP.md) | Spring + Docker + AWS RDS 환경 구성 | 초급~중급 | 25분 |
| [GITHUB_ACTIONS_TUTORIAL.md](GITHUB_ACTIONS_TUTORIAL.md) | GitHub Actions CI/CD 가이드 | 중급 | 15분 |
| [DOCKER_VS_CI_COMPARISON.md](DOCKER_VS_CI_COMPARISON.md) | Docker와 CI/CD 비교 분석 | 중급 | 15분 |
| [SPRING_TEST_MODULARIZATION.md](SPRING_TEST_MODULARIZATION.md) | Spring 테스트 모듈화 전략 | 중급 | 20분 |
| [SWAGGER_TUTORIAL.md](SWAGGER_TUTORIAL.md) | Swagger/OpenAPI 3.0 적용 가이드 | 초급~중급 | 35분 |
| [CLEAN_ARCHITECTURE_TUTORIAL.md](CLEAN_ARCHITECTURE_TUTORIAL.md) | Clean Architecture 적용 가이드 | 중급 | 45분 |
| [NOTIFICATION_ARCHITECTURE_TUTORIAL.md](NOTIFICATION_ARCHITECTURE_TUTORIAL.md) | 알림 시스템 아키텍처 가이드 | 중급 | 30분 |

---

## 선행 지식

### 필수
- Git 기본 (commit, push, pull, branch)
- Java/Spring Boot 기초
- 터미널/CLI 사용법

### 권장
- Docker 기본 개념
- REST API 이해
- SQL 기초

---

## 빠른 시작 (5분)

### 1단계: 저장소 클론
```bash
git clone https://github.com/alpomjeong/action_tutorial.git
cd action_tutorial
```

### 2단계: 환경 확인
```bash
java -version    # Java 17 필요
./gradlew -v     # Gradle 8.5
```

### 3단계: 로컬 실행
```bash
# H2 인메모리 DB로 바로 실행
./gradlew bootRun --args='--spring.profiles.active=test'
```

### 4단계: 테스트 실행
```bash
./gradlew test
```

---

## 문서 기여 가이드

### 새 문서 작성 시 포함할 항목

```markdown
# 문서 제목

> 한 줄 설명

## 문서 정보
- **레벨**: 초급 / 중급 / 고급
- **예상 읽기 시간**: XX분
- **선행 지식**: 필요한 사전 지식
- **관련 문서**: 연관된 다른 문서 링크

## 목차
1. 개요
2. 본문
3. 실전 예제
4. 문제 해결
5. 참고 자료
```

---

## 자주 묻는 질문

### Q: 어떤 데이터베이스를 써야 하나요?
**A**: 개발/학습 단계는 Supabase(무료), 운영 단계는 AWS RDS 추천
→ [DATABASE_SERVICE_COMPARISON.md](DATABASE_SERVICE_COMPARISON.md) 참고

### Q: Docker 없이 개발 가능한가요?
**A**: 가능합니다. CI/CD가 잘 구성되어 있으면 로컬은 직접 설치로 개발해도 됩니다.
→ [DOCKER_VS_CI_COMPARISON.md](DOCKER_VS_CI_COMPARISON.md) 참고

### Q: 테스트 코드 중복이 너무 많아요
**A**: CrudControllerTest 추상 클래스 상속 방식을 추천합니다.
→ [SPRING_TEST_MODULARIZATION.md](SPRING_TEST_MODULARIZATION.md) 참고

### Q: Clean Architecture를 적용해야 하나요?
**A**: 프로젝트 규모에 따라 다릅니다. 소규모는 전통적 계층형으로 충분하고, 중규모 이상이면 점진적으로 도입하세요.
→ [CLEAN_ARCHITECTURE_TUTORIAL.md](CLEAN_ARCHITECTURE_TUTORIAL.md) 참고

---

## 문서 업데이트 이력

| 날짜 | 문서 | 변경 내용 |
|------|------|----------|
| 2025-01-01 | NOTIFICATION_ARCHITECTURE_TUTORIAL.md | 알림 시스템 아키텍처 튜토리얼 신규 생성 |
| 2025-01-01 | CLEAN_ARCHITECTURE_TUTORIAL.md | Clean Architecture 튜토리얼 신규 생성 |
| 2025-01-01 | SWAGGER_TUTORIAL.md | Swagger/OpenAPI 튜토리얼 신규 생성 |
| 2025-01-01 | POSTGRESQL_TUTORIAL.md | PostgreSQL 입문 튜토리얼 신규 생성 |
| 2025-01-01 | SPRING_DOCKER_SETUP.md | Supabase → AWS RDS PostgreSQL로 변경 |
| 2025-01-01 | 전체 | 문서 품질 개선 및 연결성 강화 |
| 2025-01-01 | INDEX.md | 신규 생성 |
| 2025-01-01 | DATABASE_SERVICE_COMPARISON.md | MySQL vs PostgreSQL 비교 추가 |
