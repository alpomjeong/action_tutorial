# PostgreSQL 입문 가이드: MySQL 개발자를 위한 완벽 튜토리얼

> MySQL에서 PostgreSQL로 전환하거나 처음 시작하는 개발자를 위한 종합 가이드

## 문서 정보

| 항목 | 내용 |
|------|------|
| **레벨** | 초급 ~ 중급 |
| **예상 읽기 시간** | 40분 |
| **선행 지식** | SQL 기초, MySQL 기본 경험 (선택) |
| **최종 업데이트** | 2025년 1월 |

### 관련 문서
- [INDEX.md](INDEX.md) - 전체 문서 가이드
- [DATABASE_SERVICE_COMPARISON.md](DATABASE_SERVICE_COMPARISON.md) - DB 서비스 비교
- [SPRING_DOCKER_SETUP.md](SPRING_DOCKER_SETUP.md) - Docker + AWS RDS 설정

---

## 목차

1. [PostgreSQL vs MySQL 핵심 비교](#1-postgresql-vs-mysql-핵심-비교)
2. [문법 차이점](#2-문법-차이점)
3. [데이터 타입 차이](#3-데이터-타입-차이)
4. [PostgreSQL 전용 기능](#4-postgresql-전용-기능)
5. [DDL 차이점](#5-ddl-차이점)
6. [마이그레이션 주의사항](#6-마이그레이션-주의사항)
7. [성능 튜닝](#7-성능-튜닝)
8. [도구 비교](#8-도구-비교)
9. [JPA/Hibernate 설정](#9-jpahibernate-설정)
10. [빠른 참조 치트시트](#10-빠른-참조-치트시트)

---

## 1. PostgreSQL vs MySQL 핵심 비교

### 한눈에 보는 비교표

| 기능 | PostgreSQL | MySQL |
|------|------------|-------|
| **Auto Increment** | `SERIAL`, `IDENTITY` | `AUTO_INCREMENT` |
| **문자열 연결** | `\|\|` 연산자, `CONCAT()` | `CONCAT()` |
| **Boolean 타입** | 네이티브 `BOOLEAN` | `TINYINT(1)` |
| **대소문자 구분** | 소문자로 변환 | OS 의존적 |
| **식별자 인용** | 큰따옴표 `"` | 백틱 `` ` `` |
| **UNSIGNED 정수** | 미지원 | 지원 |
| **배열 타입** | 네이티브 `ARRAY` | 미지원 (JSON 사용) |
| **UUID 타입** | 네이티브 `UUID` | 미지원 (BINARY/VARCHAR 사용) |
| **JSON 타입** | `JSON`, `JSONB` | `JSON` |
| **DDL 트랜잭션** | 지원 | 미지원 |
| **부분 인덱스** | 지원 | 미지원 |

### 언제 PostgreSQL을 선택해야 하는가?

```
PostgreSQL이 유리한 경우:
├── 복잡한 쿼리와 조인이 많은 경우
├── JSONB 데이터 처리가 필요한 경우
├── 전문 검색(Full-text Search)이 필요한 경우
├── 지리 정보 처리 (PostGIS)
├── 무한 스크롤/커서 기반 페이지네이션
└── 데이터 무결성이 중요한 엔터프라이즈 앱

MySQL이 유리한 경우:
├── 단순 CRUD 위주의 앱
├── 읽기 중심 워크로드
├── 기존 MySQL 생태계 활용
└── 호스팅 옵션이 제한적인 경우
```

---

## 2. 문법 차이점

### 2.1 AUTO_INCREMENT vs SERIAL/IDENTITY

**MySQL:**
```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);
```

**PostgreSQL (SERIAL 방식):**
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255)
);
```

**PostgreSQL 10+ (IDENTITY 방식 - SQL 표준):**
```sql
CREATE TABLE users (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255)
);
```

**SERIAL 타입 종류:**

| 타입 | 크기 | 범위 |
|------|------|------|
| `SMALLSERIAL` | 2바이트 | 1 ~ 32,767 |
| `SERIAL` | 4바이트 | 1 ~ 2,147,483,647 |
| `BIGSERIAL` | 8바이트 | 1 ~ 9,223,372,036,854,775,807 |

### 2.2 문자열 연결

**MySQL:**
```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users;
SELECT CONCAT_WS(', ', city, state, country) AS location FROM addresses;
```

**PostgreSQL:**
```sql
-- || 연산자 사용 (권장)
SELECT first_name || ' ' || last_name AS full_name FROM users;

-- CONCAT() 함수도 지원 (PostgreSQL 9.1+)
SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users;
```

**NULL 처리 차이:**

| 방식 | NULL 동작 |
|------|-----------|
| `\|\|` 연산자 (PostgreSQL) | 하나라도 NULL이면 결과가 NULL |
| `CONCAT()` (둘 다) | NULL을 무시하고 연결 |

```sql
-- PostgreSQL에서 NULL 안전하게 처리
SELECT COALESCE(first_name, '') || ' ' || COALESCE(last_name, '') FROM users;
```

### 2.3 Boolean 처리

**PostgreSQL (네이티브 Boolean):**
```sql
CREATE TABLE tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    is_complete BOOLEAN DEFAULT FALSE
);

-- 다양한 리터럴 지원
INSERT INTO tasks (title, is_complete) VALUES ('Task 1', TRUE);
INSERT INTO tasks (title, is_complete) VALUES ('Task 2', 'yes');
INSERT INTO tasks (title, is_complete) VALUES ('Task 3', '1');

-- 쿼리
SELECT * FROM tasks WHERE is_complete IS TRUE;
SELECT * FROM tasks WHERE is_complete = TRUE;
SELECT * FROM tasks WHERE NOT is_complete;
```

**PostgreSQL Boolean 리터럴:**
- TRUE: `true`, `'true'`, `'t'`, `'yes'`, `'y'`, `'1'`
- FALSE: `false`, `'false'`, `'f'`, `'no'`, `'n'`, `'0'`

**MySQL (TINYINT(1) 사용):**
```sql
CREATE TABLE tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    is_complete BOOLEAN DEFAULT FALSE  -- 실제로는 TINYINT(1)
);

SELECT * FROM tasks WHERE is_complete = 1;
SELECT * FROM tasks WHERE is_complete = TRUE;  -- TRUE = 1
```

### 2.4 대소문자 구분

**PostgreSQL:**
```sql
-- 인용 없는 식별자는 소문자로 변환됨
CREATE TABLE Customer (ID int, UserName varchar(50));
-- 실제 생성: customer(id, username)

SELECT * FROM Customer;      -- 작동 (customer로 변환)
SELECT * FROM CUSTOMER;      -- 작동 (customer로 변환)
SELECT * FROM "Customer";    -- 에러! "Customer" 테이블 없음

-- 대소문자 유지하려면 큰따옴표 사용
CREATE TABLE "Customer" ("ID" int, "UserName" varchar(50));
SELECT * FROM "Customer";    -- 항상 따옴표 필요
```

**MySQL:**
- Windows: 기본적으로 대소문자 구분 안 함
- Linux: 기본적으로 대소문자 구분
- `lower_case_table_names` 설정으로 제어

**권장사항:** 두 데이터베이스 모두 `snake_case` 소문자 사용

### 2.5 식별자 인용

| 데이터베이스 | 인용 문자 | SQL 표준 |
|-------------|----------|----------|
| PostgreSQL | 큰따옴표 `"` | O |
| MySQL | 백틱 `` ` `` | X |

```sql
-- PostgreSQL
SELECT "column name", "SELECT" FROM "my table";

-- MySQL
SELECT `column name`, `SELECT` FROM `my table`;
```

---

## 3. 데이터 타입 차이

### 3.1 정수 타입

| 타입 | 크기 | PostgreSQL | MySQL |
|------|------|------------|-------|
| TINYINT | 1바이트 | 없음 | O |
| SMALLINT | 2바이트 | O | O |
| MEDIUMINT | 3바이트 | 없음 | O |
| INT/INTEGER | 4바이트 | O | O |
| BIGINT | 8바이트 | O | O |

**UNSIGNED 처리:**
```sql
-- MySQL
CREATE TABLE products (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    quantity SMALLINT UNSIGNED
);

-- PostgreSQL (UNSIGNED 없음, CHECK 제약조건 사용)
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    quantity SMALLINT CHECK (quantity >= 0)
);
```

### 3.2 문자열 타입

**PostgreSQL:**

| 타입 | 설명 | 최대 길이 |
|------|------|-----------|
| `CHAR(n)` | 고정 길이, 공백 패딩 | 지정된 n |
| `VARCHAR(n)` | 가변 길이, 제한 있음 | 지정된 n |
| `VARCHAR` | 가변 길이, 제한 없음 | ~1GB |
| `TEXT` | 가변 길이, 제한 없음 | ~1GB |

```sql
-- PostgreSQL: VARCHAR과 TEXT는 기능적으로 동일
CREATE TABLE posts (
    title VARCHAR(255),
    content TEXT
);
```

**MySQL:**

| 타입 | 최대 길이 |
|------|-----------|
| `CHAR(n)` | 255자 |
| `VARCHAR(n)` | 65,535바이트 |
| `TEXT` | 65,535바이트 |
| `MEDIUMTEXT` | 16MB |
| `LONGTEXT` | 4GB |

**성능 참고:** PostgreSQL에서는 `CHAR`, `VARCHAR`, `TEXT` 간 성능 차이 없음

### 3.3 날짜/시간 타입

**PostgreSQL:**

| 타입 | 저장 크기 | 설명 |
|------|----------|------|
| `DATE` | 4바이트 | 날짜만 |
| `TIME` | 8바이트 | 시간만 |
| `TIMESTAMP` | 8바이트 | 날짜+시간 (타임존 없음) |
| `TIMESTAMPTZ` | 8바이트 | 날짜+시간 (타임존 있음) **권장** |
| `INTERVAL` | 16바이트 | 시간 간격 |

```sql
-- PostgreSQL
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    event_date DATE,
    created_at TIMESTAMPTZ DEFAULT NOW()  -- 타임존 포함 권장
);

-- 특수 값
INSERT INTO events (event_date) VALUES ('today');
INSERT INTO events (event_date) VALUES ('tomorrow');
INSERT INTO events (event_date) VALUES ('infinity');
```

**MySQL:**

| 타입 | 범위 | 주의사항 |
|------|------|---------|
| `DATE` | 1000-01-01 ~ 9999-12-31 | |
| `DATETIME` | 1000-01-01 ~ 9999-12-31 | 타임존 변환 없음 |
| `TIMESTAMP` | 1970-01-01 ~ **2038-01-19** | 2038년 문제! |

**핵심 차이:**

| 기능 | PostgreSQL | MySQL |
|------|------------|-------|
| 타임존 지원 | `TIMESTAMPTZ` | 제한적 |
| INTERVAL 타입 | 네이티브 | 없음 |
| 2038년 문제 | 없음 | `TIMESTAMP` 타입에 있음 |

### 3.4 JSON vs JSONB

**PostgreSQL의 두 가지 JSON 타입:**

| 특성 | `JSON` | `JSONB` |
|------|--------|---------|
| 저장 방식 | 텍스트 그대로 | 바이너리 분해 |
| 삽입 속도 | 빠름 | 느림 (변환 오버헤드) |
| 쿼리 속도 | 느림 (재파싱 필요) | 빠름 |
| 인덱싱 | 불가 | GIN 인덱스 가능 |
| 키 순서 | 보존 | 보존 안 됨 |
| 중복 키 | 보존 | 마지막 값만 유지 |

**JSONB 사용 예시:**
```sql
CREATE TABLE api_logs (
    id SERIAL PRIMARY KEY,
    response JSONB
);

-- GIN 인덱스 생성
CREATE INDEX idx_response ON api_logs USING GIN (response);

-- 쿼리
SELECT * FROM api_logs WHERE response @> '{"status": "success"}';
SELECT response->>'status' FROM api_logs;
SELECT response->'data'->>'name' FROM api_logs;
```

**JSONB 연산자:**

| 연산자 | 설명 | 반환 타입 |
|--------|------|----------|
| `->` | 키로 JSON 객체 추출 | JSON |
| `->>` | 키로 텍스트 추출 | TEXT |
| `#>` | 경로로 JSON 추출 | JSON |
| `#>>` | 경로로 텍스트 추출 | TEXT |
| `@>` | 포함 여부 확인 | BOOLEAN |
| `?` | 키 존재 여부 | BOOLEAN |

```sql
-- 실용적인 예시
SELECT * FROM products WHERE attributes @> '{"brand": "Apple"}';
SELECT * FROM products WHERE attributes ? 'warranty';
SELECT attributes #>> '{specs, cpu}' FROM products;
```

**MySQL JSON:**
```sql
-- MySQL
SELECT * FROM api_logs WHERE response->>'$.status' = 'success';
SELECT JSON_EXTRACT(response, '$.data.name') FROM api_logs;
```

**권장사항:** PostgreSQL에서는 대부분의 경우 `JSONB` 사용

### 3.5 배열 타입 (PostgreSQL 전용)

MySQL에는 배열 타입이 없음. PostgreSQL만의 강력한 기능.

```sql
-- 배열 컬럼 선언
CREATE TABLE contacts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    phones TEXT[],
    scores INTEGER[]
);

-- 데이터 삽입
INSERT INTO contacts (name, phones, scores) VALUES
('John', ARRAY['010-1234-5678', '02-111-2222'], ARRAY[85, 90, 92]);

-- 중괄호 문법도 가능
INSERT INTO contacts (name, phones, scores) VALUES
('Jane', '{"010-9999-8888"}', '{88, 95}');

-- 배열 쿼리 (1부터 시작!)
SELECT name, phones[1] AS primary_phone FROM contacts;
SELECT * FROM contacts WHERE 90 = ANY(scores);
SELECT * FROM contacts WHERE phones @> ARRAY['010-1234-5678'];
SELECT * FROM contacts WHERE phones && ARRAY['010-1234-5678', '010-0000-0000'];
```

**배열 연산자:**

| 연산자 | 설명 |
|--------|------|
| `= ANY(array)` | 배열에 값이 있는지 |
| `@>` | 배열 포함 |
| `<@` | 배열에 포함됨 |
| `&&` | 배열 겹침 (공통 요소 있음) |
| `\|\|` | 배열 연결 |

### 3.6 UUID 타입

**PostgreSQL (네이티브):**
```sql
-- PostgreSQL 13+
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id INT
);

-- PostgreSQL 13 미만
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id INT
);
```

**MySQL (네이티브 타입 없음):**
```sql
-- BINARY(16) 사용 (효율적)
CREATE TABLE sessions (
    id BINARY(16) PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
    user_id INT
);

-- 또는 VARCHAR(36) (덜 효율적)
CREATE TABLE sessions (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id INT
);
```

---

## 4. PostgreSQL 전용 기능

### 4.1 전문 검색 (Full-Text Search)

외부 도구 없이 강력한 전문 검색 기능 제공.

**핵심 개념:**
- `tsvector`: 검색에 최적화된 정규화된 단어 목록
- `tsquery`: 검색 쿼리
- `@@`: 매칭 연산자

```sql
-- 기본 검색
SELECT 'PostgreSQL is awesome' @@ to_tsquery('postgresql');  -- true

-- 테이블에 적용
CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200),
    body TEXT,
    search_vector TSVECTOR
);

-- GIN 인덱스 생성
CREATE INDEX articles_search_idx ON articles USING GIN(search_vector);

-- 자동 업데이트 트리거
CREATE OR REPLACE FUNCTION update_search_vector() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.body, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER articles_search_update
    BEFORE INSERT OR UPDATE ON articles
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

-- 검색 쿼리
SELECT title, ts_rank(search_vector, query) AS rank
FROM articles, to_tsquery('english', 'postgresql & database') AS query
WHERE search_vector @@ query
ORDER BY rank DESC;
```

**tsquery 연산자:**

| 연산자 | 설명 |
|--------|------|
| `&` | AND |
| `\|` | OR |
| `!` | NOT |
| `<->` | 인접 (바로 옆) |

### 4.2 윈도우 함수 (Window Functions)

행을 그룹화하지 않고 집계 계산 수행.

```sql
-- 부서별 평균과 비교
SELECT
    name,
    department,
    salary,
    AVG(salary) OVER (PARTITION BY department) AS dept_avg,
    salary - AVG(salary) OVER (PARTITION BY department) AS diff
FROM employees;

-- 순위 매기기
SELECT
    name,
    department,
    salary,
    ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS rank
FROM employees;

-- 누적 합계
SELECT
    sale_date,
    amount,
    SUM(amount) OVER (ORDER BY sale_date) AS running_total
FROM sales;

-- 이전/다음 행 참조
SELECT
    sale_date,
    amount,
    LAG(amount, 1) OVER (ORDER BY sale_date) AS prev_amount,
    LEAD(amount, 1) OVER (ORDER BY sale_date) AS next_amount,
    amount - LAG(amount, 1) OVER (ORDER BY sale_date) AS change
FROM sales;

-- 부서별 Top 3
SELECT * FROM (
    SELECT
        name, department, salary,
        ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS rn
    FROM employees
) ranked
WHERE rn <= 3;
```

**주요 윈도우 함수:**

| 함수 | 설명 |
|------|------|
| `ROW_NUMBER()` | 순차 번호 |
| `RANK()` | 순위 (동점시 건너뜀) |
| `DENSE_RANK()` | 순위 (동점시 안 건너뜀) |
| `LAG(col, n)` | n행 이전 값 |
| `LEAD(col, n)` | n행 이후 값 |
| `FIRST_VALUE()` | 윈도우 첫 번째 값 |
| `SUM/AVG/COUNT() OVER` | 누적 집계 |

### 4.3 CTE (Common Table Expressions)

```sql
-- 기본 CTE
WITH active_users AS (
    SELECT * FROM users WHERE status = 'active'
),
recent_orders AS (
    SELECT * FROM orders WHERE created_at > NOW() - INTERVAL '30 days'
)
SELECT au.name, COUNT(ro.id) AS order_count
FROM active_users au
LEFT JOIN recent_orders ro ON au.id = ro.user_id
GROUP BY au.id, au.name;

-- 재귀 CTE (조직도)
WITH RECURSIVE org_chart AS (
    -- 앵커: 최상위 (매니저 없음)
    SELECT id, name, manager_id, 1 AS level
    FROM employees
    WHERE manager_id IS NULL

    UNION ALL

    -- 재귀: 하위 직원들
    SELECT e.id, e.name, e.manager_id, oc.level + 1
    FROM employees e
    JOIN org_chart oc ON e.manager_id = oc.id
)
SELECT * FROM org_chart ORDER BY level, name;
```

### 4.4 UPSERT (ON CONFLICT)

```sql
-- 충돌 시 무시
INSERT INTO inventory (product_id, product_name, quantity)
VALUES (1, 'Widget', 100)
ON CONFLICT (product_id) DO NOTHING;

-- 충돌 시 업데이트
INSERT INTO inventory (product_id, product_name, quantity)
VALUES (1, 'Widget', 50)
ON CONFLICT (product_id)
DO UPDATE SET
    quantity = inventory.quantity + EXCLUDED.quantity,
    updated_at = NOW();

-- 조건부 업데이트
INSERT INTO inventory (product_id, product_name, quantity)
VALUES (1, 'Widget', 50)
ON CONFLICT (product_id)
DO UPDATE SET quantity = EXCLUDED.quantity
WHERE inventory.quantity < EXCLUDED.quantity;  -- 새 값이 더 클 때만

-- RETURNING으로 결과 확인
INSERT INTO inventory (product_id, product_name, quantity)
VALUES (1, 'Widget', 100)
ON CONFLICT (product_id)
DO UPDATE SET quantity = EXCLUDED.quantity
RETURNING *, xmax = 0 AS was_inserted;
```

### 4.5 Materialized View

쿼리 결과를 물리적으로 저장하여 빠른 조회 제공.

```sql
-- Materialized View 생성
CREATE MATERIALIZED VIEW sales_summary AS
SELECT
    date_trunc('month', sale_date) AS month,
    region,
    SUM(amount) AS total_sales,
    COUNT(*) AS transaction_count
FROM sales
GROUP BY date_trunc('month', sale_date), region;

-- 새로고침 (기본: 쿼리 차단)
REFRESH MATERIALIZED VIEW sales_summary;

-- 동시 새로고침 (쿼리 허용, UNIQUE 인덱스 필요)
CREATE UNIQUE INDEX sales_summary_idx ON sales_summary (month, region);
REFRESH MATERIALIZED VIEW CONCURRENTLY sales_summary;
```

---

## 5. DDL 차이점

### 5.1 테이블 생성

```sql
-- PostgreSQL
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    department_id INT REFERENCES departments(id),
    salary NUMERIC(10, 2),
    is_active BOOLEAN DEFAULT TRUE,
    tags TEXT[],
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- MySQL
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    department_id INT,
    salary DECIMAL(10, 2),
    is_active TINYINT(1) DEFAULT 1,
    tags JSON,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB;
```

### 5.2 테이블 수정

**컬럼 타입 변경:**
```sql
-- PostgreSQL
ALTER TABLE employees ALTER COLUMN salary TYPE NUMERIC(12, 2);
ALTER TABLE employees ALTER COLUMN name TYPE VARCHAR(200);

-- MySQL (모든 속성 재지정 필요)
ALTER TABLE employees MODIFY COLUMN salary DECIMAL(12, 2);
ALTER TABLE employees MODIFY COLUMN name VARCHAR(200) NOT NULL;
```

**컬럼 추가/삭제:**
```sql
-- PostgreSQL
ALTER TABLE employees ADD COLUMN hire_date DATE;
ALTER TABLE employees DROP COLUMN hire_date;
ALTER TABLE employees RENAME COLUMN name TO full_name;

-- MySQL
ALTER TABLE employees ADD COLUMN hire_date DATE;
ALTER TABLE employees DROP COLUMN hire_date;
ALTER TABLE employees CHANGE COLUMN name full_name VARCHAR(100) NOT NULL;
```

**트랜잭션 DDL:**
```sql
-- PostgreSQL: DDL 롤백 가능!
BEGIN;
    ALTER TABLE employees ADD COLUMN temp INT;
    ALTER TABLE employees DROP COLUMN temp;
ROLLBACK;  -- 변경 취소됨

-- MySQL: DDL은 자동 커밋, 롤백 불가
START TRANSACTION;
    ALTER TABLE employees ADD COLUMN temp INT;  -- 즉시 커밋!
ROLLBACK;  -- DDL에는 효과 없음
```

### 5.3 인덱스 생성

```sql
-- 기본 인덱스 (둘 다 동일)
CREATE INDEX idx_name ON employees (name);
CREATE UNIQUE INDEX idx_email ON employees (email);

-- 인덱스 삭제
-- PostgreSQL
DROP INDEX idx_name;

-- MySQL (테이블 지정 필요)
DROP INDEX idx_name ON employees;

-- PostgreSQL 전용 기능
-- 부분 인덱스
CREATE INDEX idx_active ON employees (name) WHERE is_active = TRUE;

-- 표현식 인덱스
CREATE INDEX idx_lower_email ON employees (LOWER(email));

-- 동시 생성 (락 없음)
CREATE INDEX CONCURRENTLY idx_name ON employees (name);

-- GIN 인덱스 (JSONB용)
CREATE INDEX idx_metadata ON employees USING GIN (metadata);
```

---

## 6. 마이그레이션 주의사항

### 6.1 예약어 차이

PostgreSQL 예약어: ~96개, MySQL 예약어: ~733개

**자주 문제되는 예약어:**
- `user`, `order`, `offset`, `limit`, `group`

```sql
-- 큰따옴표로 이스케이프
CREATE TABLE "user" (
    id SERIAL PRIMARY KEY,
    "order" INT
);
```

### 6.2 NULL과 빈 문자열

```sql
-- PostgreSQL: NULL과 ''는 다름
INSERT INTO test (col) VALUES ('');    -- 빈 문자열
INSERT INTO test (col) VALUES (NULL);  -- NULL

-- NULL 연결 시 결과도 NULL
SELECT 'Hello' || NULL;  -- NULL 반환

-- 안전한 처리
SELECT COALESCE(column_name, 'default') FROM table;
SELECT NULLIF(column_name, '') FROM table;  -- 빈 문자열을 NULL로
```

### 6.3 날짜 함수 변환

```sql
-- MySQL
SELECT DATE_ADD(created_at, INTERVAL 1 DAY);
SELECT DATEDIFF(end_date, start_date);

-- PostgreSQL
SELECT created_at + INTERVAL '1 day';
SELECT end_date - start_date;  -- INTERVAL 반환
SELECT DATE_PART('day', end_date - start_date);  -- 숫자 반환
```

### 6.4 인코딩

- MySQL: `utf8`은 3바이트만 지원, `utf8mb4` 필요 (이모지 등)
- PostgreSQL: `UTF8`이 완전한 유니코드 지원

### 6.5 마이그레이션 체크리스트

- [ ] 예약어 검토 (`user`, `order`, `offset` 등)
- [ ] 백틱(`) → 큰따옴표(") 변환
- [ ] 식별자 소문자 사용
- [ ] `DATETIME` → `TIMESTAMPTZ` 변경
- [ ] `utf8mb4` → `UTF8` 인코딩
- [ ] 날짜 함수 변환 (`DATE_ADD` → `+ INTERVAL`)
- [ ] `AUTO_INCREMENT` → `SERIAL`/`IDENTITY`

---

## 7. 성능 튜닝

### 7.1 VACUUM과 ANALYZE

PostgreSQL은 MVCC로 "dead tuple"이 생김. VACUUM으로 정리 필요.

```sql
-- 수동 실행 (보통 불필요)
VACUUM;                      -- dead tuple 정리
VACUUM FULL;                 -- 전체 재작성 (배타적 락)
VACUUM ANALYZE;              -- 정리 + 통계 업데이트
ANALYZE;                     -- 통계만 업데이트
```

**Autovacuum 설정 (postgresql.conf):**
```ini
autovacuum = on                        # 반드시 켜두기
autovacuum_vacuum_scale_factor = 0.2   # 20% 변경 시 vacuum
autovacuum_analyze_scale_factor = 0.1  # 10% 변경 시 analyze
```

**대용량 테이블 튜닝:**
```sql
ALTER TABLE large_table SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 1000
);
```

### 7.2 메모리 설정

```ini
# 16GB RAM 시스템 예시

# 메인 캐시 (RAM의 25%, 최대 8GB)
shared_buffers = 4GB

# 쿼리 플래너 힌트 (RAM의 50-75%)
effective_cache_size = 12GB

# 정렬/해시 작업당 메모리
work_mem = 32MB

# 유지보수 작업 메모리
maintenance_work_mem = 512MB
```

**설정 도구:** [PGTune](https://pgtune.leopard.in.ua/)

### 7.3 연결 제한

```sql
-- 현재 설정 확인
SHOW max_connections;

-- 현재 연결 수
SELECT count(*) FROM pg_stat_activity;
```

**권장사항:**
- `max_connections`를 100-200 유지
- 커넥션 풀러 사용 (PgBouncer, pgpool-II)
- 연결 하나당 5-10MB 메모리 소모

### 7.4 EXPLAIN ANALYZE

```sql
-- 실행 계획만
EXPLAIN SELECT * FROM orders WHERE status = 'pending';

-- 실제 실행 + 시간 측정
EXPLAIN ANALYZE SELECT * FROM orders WHERE status = 'pending';

-- 상세 출력
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT * FROM orders WHERE status = 'pending';
```

**출력 해석:**
```
Seq Scan on orders  (cost=0.00..1234.00 rows=100 width=50) (actual time=0.01..5.23 rows=98 loops=1)
  Filter: (status = 'pending')
  Rows Removed by Filter: 9902
Planning Time: 0.123 ms
Execution Time: 5.456 ms
```

- `Seq Scan`: 전체 테이블 스캔 (인덱스 필요할 수 있음)
- `rows=100` vs `actual rows=98`: 추정 vs 실제 (차이 크면 ANALYZE 실행)
- `Rows Removed by Filter: 9902`: 많은 행 필터링됨 (인덱스 후보)

**시각화 도구:**
- [explain.depesz.com](https://explain.depesz.com/)
- [explain.dalibo.com](https://explain.dalibo.com/)

---

## 8. 도구 비교

### 8.1 GUI 도구

| 기능 | pgAdmin 4 | MySQL Workbench |
|------|-----------|-----------------|
| 아키텍처 | 웹 기반 (Python) | 네이티브 앱 |
| 시작 속도 | 느림 | 빠름 |
| DB 모델링 | 없음 | 내장 ERD |
| 원격 접속 | 웹이라 유리 | 가능 |
| 가격 | 무료 | 무료 |

**대안:**
- DBeaver (무료, 다중 DB 지원)
- DataGrip (유료, JetBrains)
- TablePlus (유료, macOS 최적화)

### 8.2 CLI 명령어 비교

| 작업 | mysql | psql |
|------|-------|------|
| 접속 | `mysql -u user -p db` | `psql -U user -d db` |
| DB 목록 | `SHOW DATABASES;` | `\l` |
| DB 선택 | `USE dbname;` | `\c dbname` |
| 테이블 목록 | `SHOW TABLES;` | `\dt` |
| 테이블 구조 | `DESCRIBE table;` | `\d table` |
| 인덱스 목록 | `SHOW INDEX FROM table;` | `\di` |
| 종료 | `exit;` | `\q` |
| SQL 파일 실행 | `source file.sql;` | `\i file.sql` |

### 8.3 유용한 psql 명령어

```
\l              데이터베이스 목록
\l+             데이터베이스 목록 (크기 포함)
\c dbname       데이터베이스 전환
\dt             테이블 목록
\dt+            테이블 목록 (크기 포함)
\d tablename    테이블 구조
\d+ tablename   테이블 상세 구조
\dn             스키마 목록
\di             인덱스 목록
\dv             뷰 목록
\df             함수 목록
\du             사용자 목록

\x              확장 출력 토글 (세로 표시)
\timing         쿼리 시간 측정 토글
\e              외부 에디터로 쿼리 편집
\i filename     SQL 파일 실행
\o filename     결과를 파일로 저장

\?              psql 명령어 도움말
\h              SQL 명령어 도움말
\h SELECT       특정 SQL 도움말
```

### 8.4 백업/복원

```bash
# PostgreSQL
pg_dump dbname > backup.sql                    # 백업
pg_dump -Fc dbname > backup.dump               # 커스텀 포맷 (권장)
pg_dumpall > all_databases.sql                 # 전체 백업
psql dbname < backup.sql                       # 복원
pg_restore -d dbname backup.dump               # 커스텀 포맷 복원
pg_restore -j 4 -d dbname backup.dump          # 병렬 복원

# MySQL
mysqldump dbname > backup.sql
mysql dbname < backup.sql
```

---

## 9. JPA/Hibernate 설정

### 9.1 기본 설정

**application.properties:**
```properties
# PostgreSQL 연결
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate Dialect (자동 감지)
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 9.2 ID 생성 전략

**권장: SEQUENCE 사용**

IDENTITY보다 SEQUENCE가 좋은 이유:
1. 배치 INSERT 최적화 가능
2. INSERT 전에 ID 알 수 있음
3. DB 왕복 최소화

```java
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(
        name = "order_seq",
        sequenceName = "order_sequence",
        allocationSize = 50  // DB INCREMENT와 일치시킬 것
    )
    private Long id;
}
```

**PostgreSQL에서 시퀀스 생성:**
```sql
CREATE SEQUENCE order_sequence
    START WITH 1
    INCREMENT BY 50
    CACHE 50;
```

### 9.3 PostgreSQL 전용 타입 매핑

```java
// TEXT 타입
@Column(columnDefinition = "TEXT")
private String description;

// 또는 DB 독립적으로
@Lob
private String description;

// JSONB (Hibernate Types 라이브러리 필요)
// com.vladmihalcea:hibernate-types-60
@Type(JsonBinaryType.class)
@Column(columnDefinition = "jsonb")
private Map<String, Object> metadata;

// 배열 (Hibernate Types 라이브러리 필요)
@Type(StringArrayType.class)
@Column(columnDefinition = "text[]")
private String[] tags;

// UUID
@Id
@GeneratedValue(strategy = GenerationType.UUID)
@Column(columnDefinition = "uuid")
private UUID id;
```

### 9.4 JPA 체크리스트

- [ ] `PostgreSQLDialect` 설정
- [ ] `GenerationType.SEQUENCE` 사용 (IDENTITY 피하기)
- [ ] `allocationSize`와 DB 시퀀스 INCREMENT 일치
- [ ] PostgreSQL 전용 타입 매핑 설정

---

## 10. 빠른 참조 치트시트

### JSONB 연산자
```sql
data -> 'key'         -- JSON 값 추출
data ->> 'key'        -- TEXT 값 추출
data @> '{"a":1}'     -- 포함 여부
data ? 'key'          -- 키 존재 여부
```

### 배열 연산자
```sql
'x' = ANY(array)      -- 배열에 요소 있는지
array @> ARRAY['x']   -- 배열 포함
array && ARRAY['x']   -- 배열 겹침
```

### 전문 검색
```sql
to_tsvector('text') @@ to_tsquery('word')
```

### 윈도우 함수
```sql
ROW_NUMBER() OVER (PARTITION BY col ORDER BY col2)
SUM(amount) OVER (ORDER BY date)  -- 누적 합계
```

### CTE
```sql
WITH cte AS (SELECT ...) SELECT * FROM cte;
```

### UPSERT
```sql
INSERT INTO t VALUES (...)
ON CONFLICT (id) DO UPDATE SET col = EXCLUDED.col;
```

### 유용한 psql 명령어
```
\l    DB 목록      \dt   테이블 목록
\c    DB 전환      \d    테이블 구조
\di   인덱스       \du   사용자
\x    세로 출력    \timing  시간 측정
```

---

## 참고 자료

### 공식 문서
- [PostgreSQL Documentation](https://www.postgresql.org/docs/current/)
- [MySQL Documentation](https://dev.mysql.com/doc/)

### 튜토리얼
- [PostgreSQL Tutorial](https://www.postgresqltutorial.com/)
- [Neon PostgreSQL Guides](https://neon.com/postgresql)

### 도구
- [PGTune - 설정 최적화](https://pgtune.leopard.in.ua/)
- [explain.depesz.com - 실행계획 시각화](https://explain.depesz.com/)

---

## 다음 단계

- [Docker + AWS RDS 설정](SPRING_DOCKER_SETUP.md)
- [DB 서비스 비교](DATABASE_SERVICE_COMPARISON.md)
- [GitHub Actions CI/CD](GITHUB_ACTIONS_TUTORIAL.md)
