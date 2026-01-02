# PostgreSQL: JSONB vs 컬럼 추가 방식 비교

> 유연한 데이터 저장이 필요할 때 어떤 방식을 선택해야 하는가?

## 문서 정보

| 항목 | 내용 |
|------|------|
| **레벨** | 중급 |
| **예상 읽기 시간** | 10분 |
| **선행 지식** | SQL 기초, PostgreSQL 기본 |
| **최종 업데이트** | 2025년 1월 |

### 관련 문서
- [POSTGRESQL_TUTORIAL.md](POSTGRESQL_TUTORIAL.md) - PostgreSQL 입문
- [DATABASE_SERVICE_COMPARISON.md](DATABASE_SERVICE_COMPARISON.md) - DB 서비스 비교

---

## 목차

1. [두 방식 비교 개요](#1-두-방식-비교-개요)
2. [컬럼 추가 방식](#2-컬럼-추가-방식)
3. [JSONB 방식](#3-jsonb-방식)
4. [장단점 비교표](#4-장단점-비교표)
5. [성능 비교](#5-성능-비교)
6. [언제 무엇을 선택할까?](#6-언제-무엇을-선택할까)
7. [하이브리드 방식](#7-하이브리드-방식)

---

## 1. 두 방식 비교 개요

```
상황: 상품마다 다른 속성을 저장해야 함
─────────────────────────────────────────

노트북: cpu, ram, storage, screen_size
의류:   size, color, material
식품:   expiry_date, calories, ingredients
```

### 방식 A: 컬럼 추가

```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    -- 모든 가능한 속성을 컬럼으로
    cpu VARCHAR(50),
    ram VARCHAR(50),
    storage VARCHAR(50),
    size VARCHAR(10),
    color VARCHAR(20),
    material VARCHAR(50),
    expiry_date DATE,
    calories INT
    -- ... 계속 늘어남
);
```

### 방식 B: JSONB

```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    category VARCHAR(50),
    attributes JSONB  -- 모든 속성을 JSON으로
);

-- 노트북
INSERT INTO products VALUES (1, '맥북', 'laptop',
    '{"cpu": "M3", "ram": "16GB", "storage": "512GB"}');

-- 의류
INSERT INTO products VALUES (2, '티셔츠', 'clothing',
    '{"size": "L", "color": "black", "material": "cotton"}');
```

---

## 2. 컬럼 추가 방식

### 구조

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    -- 설정 관련 컬럼들
    theme VARCHAR(20) DEFAULT 'light',
    language VARCHAR(10) DEFAULT 'ko',
    notification_email BOOLEAN DEFAULT true,
    notification_push BOOLEAN DEFAULT true,
    notification_sms BOOLEAN DEFAULT false
);
```

### 장점

```
✅ 타입 안정성
   - 컴파일/런타임에 타입 체크
   - 잘못된 데이터 입력 방지

✅ 명확한 스키마
   - 테이블 구조만 보면 데이터 파악 가능
   - 문서화 불필요

✅ 최적의 쿼리 성능
   - 인덱스 생성 간단
   - 쿼리 플래너 최적화 용이

✅ NOT NULL, UNIQUE 등 제약조건
   - 데이터 무결성 보장
   - DB 레벨에서 검증

✅ JOIN 최적화
   - 관계형 DB의 장점 활용
   - 외래키 제약조건 사용 가능
```

### 단점

```
❌ 스키마 변경 비용
   - ALTER TABLE 필요
   - 대용량 테이블은 락 발생 가능
   - 마이그레이션 스크립트 관리

❌ NULL 컬럼 증가
   - 카테고리마다 다른 필드 → 대부분 NULL
   - 스토리지 낭비 (미미하지만)

❌ 유연성 부족
   - 새 속성 추가 시 코드 + DB 둘 다 수정
   - 배포 필요

❌ 컬럼 폭발
   - 속성 종류가 많으면 컬럼 수십~수백 개
   - 관리 어려움
```

### 스키마 변경 예시

```sql
-- 새 필드 추가 시
ALTER TABLE users ADD COLUMN dark_mode BOOLEAN DEFAULT false;

-- 대용량 테이블에서는...
-- 1. 락 발생 가능
-- 2. 마이그레이션 시간 소요
-- 3. 애플리케이션 코드 수정 필요
-- 4. 배포 필요
```

---

## 3. JSONB 방식

### 구조

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    settings JSONB DEFAULT '{}'::jsonb  -- 모든 설정을 JSON으로
);

-- 데이터 삽입
INSERT INTO users (email, name, settings) VALUES (
    'user@example.com',
    '홍길동',
    '{
        "theme": "dark",
        "language": "ko",
        "notifications": {
            "email": true,
            "push": true,
            "sms": false
        }
    }'
);
```

### 장점

```
✅ 유연한 스키마
   - 스키마 변경 없이 새 필드 추가
   - 데이터마다 다른 구조 가능

✅ 빠른 개발/배포
   - ALTER TABLE 불필요
   - 코드만 수정하면 됨

✅ 중첩 데이터 저장
   - 복잡한 구조도 하나의 컬럼에
   - 정규화 불필요

✅ 다양한 연산자 지원
   - 검색, 포함 여부, 키 존재 등
   - GIN 인덱스로 빠른 검색

✅ API 친화적
   - JSON 그대로 반환 가능
   - 변환 로직 불필요
```

### 단점

```
❌ 타입 안정성 부족
   - 잘못된 데이터 들어갈 수 있음
   - 런타임에서만 오류 발견

❌ 스키마 파악 어려움
   - 어떤 필드가 있는지 DB만 보면 모름
   - 별도 문서화 필요

❌ 쿼리 복잡도 증가
   - attributes->>'cpu' 같은 문법 필요
   - 오타 발생 시 찾기 어려움

❌ 제약조건 제한
   - NOT NULL, UNIQUE 적용 어려움
   - 애플리케이션에서 검증 필요

❌ JOIN 비효율
   - JSON 내부 값으로 JOIN 시 성능 저하
   - 정규화된 구조보다 느림
```

### JSONB 쿼리 예시

```sql
-- 기본 조회
SELECT * FROM users WHERE settings->>'theme' = 'dark';

-- 중첩 데이터 조회
SELECT * FROM users WHERE settings->'notifications'->>'email' = 'true';

-- 키 존재 여부
SELECT * FROM users WHERE settings ? 'theme';

-- 부분 일치
SELECT * FROM users WHERE settings @> '{"language": "ko"}';

-- 인덱스 생성
CREATE INDEX idx_settings ON users USING GIN (settings);
CREATE INDEX idx_theme ON users ((settings->>'theme'));
```

---

## 4. 장단점 비교표

| 기준 | 컬럼 추가 방식 | JSONB 방식 |
|------|--------------|-----------|
| **타입 안정성** | ✅ 강함 | ❌ 약함 |
| **스키마 명확성** | ✅ 명확 | ❌ 불명확 |
| **유연성** | ❌ 낮음 | ✅ 높음 |
| **스키마 변경** | ❌ ALTER 필요 | ✅ 불필요 |
| **개발 속도** | ❌ 느림 | ✅ 빠름 |
| **쿼리 성능** | ✅ 최적 | ⚠️ 약간 느림 |
| **인덱스** | ✅ 간단 | ⚠️ GIN 필요 |
| **제약조건** | ✅ 가능 | ❌ 제한적 |
| **JOIN** | ✅ 효율적 | ❌ 비효율 |
| **NULL 처리** | ❌ NULL 다수 | ✅ 필요한 것만 |
| **저장 공간** | ⚠️ NULL 낭비 | ✅ 효율적 |
| **디버깅** | ✅ 쉬움 | ❌ 어려움 |

---

## 5. 성능 비교

### 조회 성능

```
단순 조회 (인덱스 있음):
──────────────────────────
컬럼 방식:  WHERE theme = 'dark'           → 1ms
JSONB:     WHERE settings->>'theme' = 'dark' → 1~2ms

→ 인덱스 있으면 큰 차이 없음
```

```
복잡한 조회:
──────────────────────────
컬럼 방식:  WHERE theme = 'dark' AND language = 'ko'
JSONB:     WHERE settings @> '{"theme": "dark", "language": "ko"}'

→ JSONB가 약간 느림 (GIN 인덱스 필요)
```

### 쓰기 성능

```
INSERT:
──────────────────────────
컬럼 방식:  개별 컬럼 삽입 → 빠름
JSONB:     JSON 파싱 필요 → 약간 느림

UPDATE:
──────────────────────────
컬럼 방식:  UPDATE ... SET theme = 'dark'
JSONB:     UPDATE ... SET settings = settings || '{"theme": "dark"}'

→ JSONB 업데이트가 약간 복잡
```

### 벤치마크 예시 (100만 행 기준)

| 작업 | 컬럼 방식 | JSONB (인덱스 O) | JSONB (인덱스 X) |
|-----|----------|-----------------|-----------------|
| 단일 필드 조회 | 5ms | 8ms | 500ms |
| 다중 필드 조회 | 10ms | 15ms | 800ms |
| INSERT | 1ms | 1.5ms | 1.5ms |
| UPDATE 단일 필드 | 2ms | 5ms | 5ms |

---

## 6. 언제 무엇을 선택할까?

### 컬럼 추가 방식 선택

```
✅ 선택 조건:
   - 스키마가 안정적 (자주 안 바뀜)
   - 데이터 무결성이 중요
   - 복잡한 JOIN이 필요
   - 타입 검증이 필수
   - 필드 수가 적음 (20개 이하)

📌 예시:
   - 사용자 기본 정보 (email, name, phone)
   - 주문 정보 (금액, 상태, 날짜)
   - 결제 정보 (카드번호, 유효기간)
```

### JSONB 방식 선택

```
✅ 선택 조건:
   - 스키마가 자주 변경됨
   - 데이터마다 필드가 다름
   - 빠른 개발/배포 필요
   - 중첩 구조 저장 필요
   - API 응답 그대로 저장

📌 예시:
   - 사용자 설정 (theme, language, notifications...)
   - 상품 속성 (카테고리마다 다름)
   - 이벤트 로그 (다양한 형태)
   - 외부 API 응답 캐싱
   - 메타데이터
```

### 결정 플로우차트

```
                    ┌─────────────────┐
                    │  새 필드 추가    │
                    │  얼마나 자주?    │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
           거의 안함      가끔 (월1회)      자주 (주1회+)
              │              │              │
              ▼              ▼              ▼
         ┌─────────┐   ┌─────────────┐  ┌─────────┐
         │  컬럼   │   │ 하이브리드  │  │  JSONB  │
         └─────────┘   └─────────────┘  └─────────┘
```

---

## 7. 하이브리드 방식

### 핵심 필드는 컬럼, 부가 정보는 JSONB

```sql
CREATE TABLE products (
    -- 핵심 필드: 컬럼으로 (검색, JOIN 필요)
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),

    -- 부가 정보: JSONB로 (유연성 필요)
    attributes JSONB DEFAULT '{}'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- 인덱스
CREATE INDEX idx_category ON products(category);  -- 컬럼 인덱스
CREATE INDEX idx_attributes ON products USING GIN (attributes);  -- JSONB 인덱스
```

### Spring Boot Entity 예시

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 핵심 필드: 컬럼
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String category;

    // 부가 정보: JSONB
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    // 특정 속성 접근 헬퍼 메서드
    public String getCpu() {
        return (String) attributes.get("cpu");
    }

    public void setCpu(String cpu) {
        attributes.put("cpu", cpu);
    }
}
```

### 장점

```
✅ 핵심 필드:
   - 타입 안정성
   - 빠른 검색/JOIN
   - 제약조건 적용

✅ JSONB 필드:
   - 유연한 확장
   - 스키마 변경 불필요
   - 카테고리별 다른 속성
```

---

## 요약

```
┌────────────────────────────────────────────────────────────┐
│                      선택 가이드                           │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  컬럼 추가 방식                                             │
│  ─────────────                                             │
│  • 안정적인 스키마                                          │
│  • 타입 검증 필수                                          │
│  • JOIN 많이 사용                                          │
│                                                            │
│  JSONB 방식                                                │
│  ───────────                                               │
│  • 자주 변하는 스키마                                       │
│  • 빠른 개발 우선                                          │
│  • 유연한 데이터 구조                                       │
│                                                            │
│  하이브리드 (권장)                                          │
│  ───────────────                                           │
│  • 핵심 = 컬럼                                             │
│  • 부가/메타데이터 = JSONB                                  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 다음 단계

- [PostgreSQL 입문](POSTGRESQL_TUTORIAL.md)
- [데이터베이스 서비스 비교](DATABASE_SERVICE_COMPARISON.md)
- [전체 문서 가이드](INDEX.md)
