# 데이터베이스 서비스 비교: Supabase vs AWS vs Google Cloud

## 개요

| 서비스 | 유형 | 핵심 특징 |
|--------|------|----------|
| **Supabase** | BaaS (Backend as a Service) | PostgreSQL + 부가기능 올인원 |
| **AWS RDS** | 관리형 DB | 다양한 DB 엔진 선택 가능 |
| **Google Cloud SQL** | 관리형 DB | GCP 생태계 통합 |

---

## 1. 가격 비교

### 1.1 무료 티어

| 항목 | Supabase | AWS RDS | Google Cloud SQL |
|------|----------|---------|------------------|
| **무료 기간** | 무기한 | 12개월 | 없음 (크레딧만) |
| **DB 용량** | 500MB | 20GB (12개월) | - |
| **Storage** | 1GB | - | - |
| **제한** | 2개 프로젝트 | t2.micro만 | $300 크레딧 (90일) |

### 1.2 유료 플랜 비교 (PostgreSQL 기준)

#### 소규모 (개인/스타트업)

| 사양 | Supabase Pro | AWS RDS | Google Cloud SQL |
|------|-------------|---------|------------------|
| **vCPU** | 2 | 2 (db.t3.small) | 2 (db-custom-2) |
| **RAM** | 4GB | 2GB | 4GB |
| **Storage** | 8GB | 20GB | 10GB |
| **월 비용** | **$25** | ~$30 | ~$35 |

#### 중규모 (성장기 서비스)

| 사양 | Supabase Team | AWS RDS | Google Cloud SQL |
|------|--------------|---------|------------------|
| **vCPU** | 4 | 4 (db.t3.medium) | 4 |
| **RAM** | 8GB | 8GB | 8GB |
| **Storage** | 100GB | 100GB | 100GB |
| **월 비용** | **$599** | ~$150 | ~$180 |

#### 대규모 (엔터프라이즈)

| 사양 | Supabase | AWS RDS | Google Cloud SQL |
|------|----------|---------|------------------|
| **vCPU** | 8+ | 8+ (db.r5.large) | 8+ |
| **RAM** | 32GB+ | 32GB+ | 32GB+ |
| **Storage** | 1TB+ | 1TB+ | 1TB+ |
| **월 비용** | 별도 문의 | ~$500+ | ~$550+ |

### 1.3 추가 비용 요소

| 항목 | Supabase | AWS RDS | Google Cloud SQL |
|------|----------|---------|------------------|
| **데이터 전송 (아웃)** | 포함 | $0.09/GB | $0.12/GB |
| **백업** | 포함 | 무료 (DB 크기까지) | $0.08/GB/월 |
| **Read Replica** | Pro 이상 | 추가 인스턴스 비용 | 추가 인스턴스 비용 |
| **자동 스케일링** | 포함 | 별도 설정 | 별도 설정 |

---

## 2. 기능 비교

### 2.1 기본 기능

| 기능 | Supabase | AWS RDS | Google Cloud SQL |
|------|----------|---------|------------------|
| **지원 DB** | PostgreSQL만 | PostgreSQL, MySQL, MariaDB, Oracle, SQL Server | PostgreSQL, MySQL, SQL Server |
| **자동 백업** | ✅ | ✅ | ✅ |
| **Point-in-time Recovery** | Pro 이상 | ✅ | ✅ |
| **Read Replica** | Pro 이상 | ✅ | ✅ |
| **Multi-AZ** | ❌ | ✅ | ✅ |
| **암호화** | ✅ | ✅ | ✅ |

### 2.2 부가 기능

| 기능 | Supabase | AWS | Google Cloud |
|------|----------|-----|--------------|
| **인증 (Auth)** | ✅ 내장 | Cognito (별도) | Firebase Auth (별도) |
| **파일 스토리지** | ✅ 내장 | S3 (별도) | Cloud Storage (별도) |
| **Realtime** | ✅ 내장 | AppSync (별도) | Firebase (별도) |
| **Edge Functions** | ✅ 내장 | Lambda (별도) | Cloud Functions (별도) |
| **API 자동 생성** | ✅ (PostgREST) | ❌ | ❌ |

### 2.3 관리 편의성

| 항목 | Supabase | AWS RDS | Google Cloud SQL |
|------|----------|---------|------------------|
| **설정 난이도** | 매우 쉬움 | 중간 | 중간 |
| **대시보드** | 직관적 | 복잡함 | 보통 |
| **SQL 에디터** | ✅ 웹 내장 | ❌ (별도 도구) | ❌ (별도 도구) |
| **스키마 시각화** | ✅ | ❌ | ❌ |
| **로그 확인** | ✅ 대시보드 | CloudWatch | Cloud Logging |

---

## 3. 장단점 상세

### 3.1 Supabase

#### 장점
```
✅ 무료 티어 넉넉 (개인/학습용 최적)
✅ 올인원 (DB + Auth + Storage + Realtime)
✅ 설정 5분 만에 완료
✅ 직관적인 대시보드
✅ 오픈소스 (Self-hosting 가능)
✅ PostgREST로 API 자동 생성
```

#### 단점
```
❌ PostgreSQL만 지원
❌ 중규모 이상에서 가격 급증 (Team $599)
❌ Multi-AZ/고가용성 옵션 제한적
❌ 리전 선택 제한적
❌ 엔터프라이즈 기능 부족
❌ Vendor lock-in (Supabase 기능 사용 시)
```

#### 적합한 경우
```
- 개인 프로젝트 / 사이드 프로젝트
- MVP / 프로토타입
- 소규모 스타트업 (시드~시리즈A)
- 빠른 개발 속도가 중요할 때
```

---

### 3.2 AWS RDS

#### 장점
```
✅ 다양한 DB 엔진 지원
✅ 가장 큰 클라우드 생태계
✅ Multi-AZ 고가용성
✅ 세밀한 설정 가능
✅ 12개월 무료 티어
✅ 글로벌 리전 다양
✅ 엔터프라이즈 검증됨
```

#### 단점
```
❌ 복잡한 설정
❌ 추가 서비스 별도 구성 필요 (Auth, Storage 등)
❌ 가격 예측 어려움 (숨겨진 비용)
❌ 학습 곡선 높음
❌ 과금 폭탄 위험 (설정 실수 시)
```

#### 적합한 경우
```
- 중규모 이상 서비스
- AWS 생태계 이미 사용 중
- 고가용성 필수
- 다양한 DB 엔진 필요
- DevOps 인력 있음
```

---

### 3.3 Google Cloud SQL

#### 장점
```
✅ GCP 서비스와 긴밀한 통합
✅ BigQuery 연동 쉬움 (분석)
✅ Kubernetes (GKE) 연동 우수
✅ 자동 스토리지 증가
✅ IAM 통합 인증
```

#### 단점
```
❌ 무료 티어 없음 (크레딧만)
❌ AWS 대비 리전 적음
❌ 커뮤니티/자료 AWS보다 적음
❌ 가격이 AWS보다 약간 높음
```

#### 적합한 경우
```
- GCP 생태계 사용 중
- BigQuery로 데이터 분석 필요
- GKE(Kubernetes) 사용
- Firebase 프로젝트 확장
```

---

## 4. 시나리오별 추천

### 4.1 개인 프로젝트 / 학습

```
추천: Supabase (무료)

이유:
- 무료로 충분
- 설정 5분
- 부가 기능 다 포함
```

### 4.2 스타트업 MVP

```
추천: Supabase Pro ($25/월)

이유:
- 빠른 개발 속도
- 인증/스토리지 고민 없음
- 나중에 이관 가능
```

### 4.3 성장기 서비스 (MAU 10만+)

```
추천: AWS RDS 또는 Supabase에서 이관

비교:
- Supabase Team: $599/월 (비쌈)
- AWS RDS: ~$150/월 (저렴)
- 직접 관리 필요하지만 비용 절감
```

### 4.4 대규모 서비스 (MAU 100만+)

```
추천: AWS RDS / Google Cloud SQL

이유:
- Multi-AZ 고가용성 필수
- Read Replica 여러 개
- 세밀한 튜닝 필요
- SLA 보장
```

---

## 5. 이관 전략

### 5.1 Supabase → AWS RDS

```bash
# 1. Supabase에서 백업
pg_dump -h db.xxx.supabase.co -U postgres -d postgres > backup.sql

# 2. AWS RDS 인스턴스 생성
aws rds create-db-instance \
  --db-instance-identifier mydb \
  --db-instance-class db.t3.small \
  --engine postgres

# 3. 데이터 복원
psql -h mydb.xxx.rds.amazonaws.com -U postgres < backup.sql

# 4. Spring application.yml 수정
spring:
  datasource:
    url: jdbc:postgresql://mydb.xxx.rds.amazonaws.com:5432/postgres
```

### 5.2 이관 체크리스트

```
□ Supabase Auth 사용 중? → Spring Security로 교체
□ Supabase Storage 사용 중? → S3로 이관
□ Supabase Realtime 사용 중? → WebSocket 직접 구현 또는 제거
□ PostgREST API 사용 중? → Spring API로 교체
□ Edge Functions 사용 중? → Lambda/Spring으로 이관
```

---

## 6. 비용 시뮬레이션

### 6.1 소규모 (월 10만 요청)

| 서비스 | DB | Storage | 기타 | 월 총액 |
|--------|-----|---------|------|--------|
| Supabase Free | $0 | $0 | $0 | **$0** |
| AWS RDS (프리티어) | $0 | $0 | $0 | **$0** |

### 6.2 중소규모 (월 100만 요청)

| 서비스 | DB | Storage | 기타 | 월 총액 |
|--------|-----|---------|------|--------|
| Supabase Pro | $25 | 포함 | 포함 | **$25** |
| AWS | $30 (RDS) | $3 (S3) | $5 (기타) | **~$40** |
| GCP | $35 | $3 | $5 | **~$45** |

### 6.3 중규모 (월 1000만 요청)

| 서비스 | DB | Storage | 기타 | 월 총액 |
|--------|-----|---------|------|--------|
| Supabase Team | $599 | 포함 | 포함 | **$599** |
| AWS | $150 | $20 | $30 | **~$200** |
| GCP | $180 | $20 | $30 | **~$230** |

---

## 7. 결론

### 선택 가이드

```
┌─────────────────────────────────────────────────┐
│                  시작 단계                       │
│                     │                           │
│         ┌──────────┴──────────┐                │
│         ▼                     ▼                │
│   개인/소규모              중규모 이상          │
│   Supabase 추천           AWS/GCP 추천         │
│         │                     │                │
│         ▼                     ▼                │
│   성장하면?              이미 클라우드 있음?    │
│   AWS/GCP 이관 고려      해당 클라우드 DB 사용  │
└─────────────────────────────────────────────────┘
```

### 핵심 요약

| 상황 | 추천 | 이유 |
|------|------|------|
| 빠른 시작, 비용 0 | Supabase | 무료 + 올인원 |
| 스타트업 초기 | Supabase Pro | $25로 충분 |
| 성장기 (비용 중요) | AWS RDS | Supabase 대비 저렴 |
| 대규모/엔터프라이즈 | AWS/GCP | 고가용성, 안정성 |
| GCP 생태계 | Cloud SQL | BigQuery 연동 |

### Spring Boot 프로젝트 권장 경로

```
Phase 1 (개발/MVP):     Supabase Free/Pro
Phase 2 (성장):         AWS RDS로 이관 고려
Phase 3 (대규모):       AWS RDS + Read Replica + Multi-AZ
```

**JPA 사용 시 이관은 URL 변경만으로 가능** → 처음엔 Supabase로 빠르게, 나중에 필요 시 이관
