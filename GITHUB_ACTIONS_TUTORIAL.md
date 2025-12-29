# GitHub Actions 튜토리얼

## 시나리오

5인 팀이 커뮤니티 웹앱의 백엔드를 Spring Boot로 개발한다.

| 팀원 | 담당 기능 |
|------|----------|
| A | 회원 인증 (Auth) |
| B | 게시판 (Board) |
| C | 댓글 (Comment) |
| D | 알림 (Notification) |
| E | 검색 (Search) |

### 워크플로우

```
feature/auth ──→ push ──→ CI 검증 ──→ PR ──→ Squash Merge ──→ main
feature/board ──→ push ──→ CI 검증 ──→ PR ──→ Squash Merge ──→ main
...
```

---

## 1. GitHub Actions란?

GitHub에서 제공하는 자동화 도구다.
코드를 push하면 자동으로 빌드, 테스트, 배포 등을 수행한다.

### 왜 필요한가?

팀원 A가 회원 인증 기능을 구현하고 push했다.
```
git push origin feature/auth
```

**GitHub Actions 없이:**
- 다른 팀원이 pull 받아서 직접 빌드/테스트해봐야 함
- "내 컴퓨터에서는 되는데?" 문제 발생
- 버그가 main에 들어갈 수 있음

**GitHub Actions 사용:**
- push하면 자동으로 빌드/테스트 실행
- 실패하면 즉시 알림
- main에는 검증된 코드만 머지 가능

---

## 2. 기본 구조

워크플로우 파일 위치: `.github/workflows/ci.yml`

```yaml
name: CI                    # 워크플로우 이름

on:                         # 언제 실행할지
  push:
    branches:
      - main
      - 'feature/**'

jobs:                       # 무엇을 실행할지
  build:
    runs-on: ubuntu-latest  # 실행 환경
    steps:                  # 실행 단계
      - name: Step 1
        run: echo "Hello"
```

### 핵심 키워드

| 키워드 | 설명 | 예시 |
|--------|------|------|
| `name` | 워크플로우/스텝 이름 | `name: Build` |
| `on` | 트리거 조건 | `on: push` |
| `jobs` | 실행할 작업 목록 | - |
| `runs-on` | 실행 환경 | `ubuntu-latest` |
| `steps` | 순차 실행할 단계들 | - |
| `uses` | 외부 액션 사용 | `uses: actions/checkout@v4` |
| `run` | 쉘 명령어 실행 | `run: ./gradlew build` |
| `with` | 액션에 옵션 전달 | `with: java-version: '17'` |

---

## 3. 트리거 (on)

### 언제 CI가 실행되는가?

```yaml
on:
  # 1. push할 때
  push:
    branches:
      - main
      - 'feature/**'      # feature/auth, feature/board 등

  # 2. PR 생성/업데이트할 때
  pull_request:
    branches:
      - main

  # 3. 수동 실행
  workflow_dispatch:
```

### 팀 시나리오에서의 흐름

```
팀원 A: feature/auth 브랜치에서 작업
        ↓
        git push origin feature/auth
        ↓
        CI 자동 실행 (on: push - feature/**)
        ↓
        빌드/테스트 통과
        ↓
        PR 생성 (feature/auth → main)
        ↓
        CI 다시 실행 (on: pull_request)
        ↓
        리뷰 후 Squash Merge
```

---

## 4. 스텝 (steps)

### uses vs run

```yaml
steps:
  # uses: 미리 만들어진 액션 사용
  - name: 코드 체크아웃
    uses: actions/checkout@v4

  # run: 직접 명령어 실행
  - name: 빌드
    run: ./gradlew build
```

### 자주 사용하는 액션

| 액션 | 용도 |
|------|------|
| `actions/checkout@v4` | 저장소 코드 가져오기 |
| `actions/setup-java@v4` | JDK 설치 |
| `actions/cache@v4` | 캐싱 (빌드 속도 향상) |
| `actions/upload-artifact@v4` | 빌드 결과물 저장 |

---

## 5. 환경 변수와 시크릿

### 환경 변수 (env)

```yaml
env:
  JAVA_VERSION: '17'

jobs:
  build:
    steps:
      - name: JDK 설정
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
```

### 시크릿 (secrets)

민감한 정보(DB 비밀번호, API 키 등)는 시크릿으로 관리한다.

**설정 방법:**
1. GitHub 저장소 → Settings → Secrets and variables → Actions
2. New repository secret 클릭
3. 이름과 값 입력

**사용:**
```yaml
steps:
  - name: 배포
    env:
      DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
    run: ./deploy.sh
```

---

## 6. 실전 예제: 커뮤니티 웹앱 CI

```yaml
name: Community App CI

on:
  push:
    branches:
      - main
      - 'feature/**'
  pull_request:
    branches:
      - main

env:
  JAVA_VERSION: '17'

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: 코드 체크아웃
        uses: actions/checkout@v4

      - name: JDK 17 설정
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Gradle 실행 권한 부여
        run: chmod +x gradlew

      - name: 빌드
        run: ./gradlew build -x test

      - name: 테스트
        run: ./gradlew test

      - name: 테스트 결과 요약
        if: always()
        run: |
          echo "## 테스트 결과" >> $GITHUB_STEP_SUMMARY
          echo "빌드 완료!" >> $GITHUB_STEP_SUMMARY
```

---

## 7. 팀 협업 시나리오 상세

### Step 1: 팀원 A가 회원 인증 기능 개발

```bash
# 브랜치 생성
git checkout -b feature/auth

# 코드 작성 후 커밋
git add .
git commit -m "feat: 로그인 기능 구현"

# push
git push origin feature/auth
```

### Step 2: CI 자동 실행

push하면 GitHub Actions가 자동으로:
1. 코드 체크아웃
2. JDK 설치
3. 빌드
4. 테스트

### Step 3: 결과 확인

GitHub 저장소 → Actions 탭에서 확인

```
✅ Build succeeded
  ✅ 코드 체크아웃
  ✅ JDK 17 설정
  ✅ 빌드
  ✅ 테스트
```

또는

```
❌ Build failed
  ✅ 코드 체크아웃
  ✅ JDK 17 설정
  ✅ 빌드
  ❌ 테스트  ← 클릭해서 에러 확인
```

### Step 4: PR 생성 및 머지

```
feature/auth → main (Pull Request)

[Squash and merge] 클릭
↓
여러 커밋이 하나로 합쳐져서 main에 반영됨
```

---

## 8. 자주 발생하는 문제와 해결

### 문제 1: gradlew 실행 권한 없음

```
Error: ./gradlew: Permission denied
```

**해결:**
```yaml
- name: Gradle 실행 권한 부여
  run: chmod +x gradlew
```

### 문제 2: 테스트 실패

```
> Task :test FAILED
```

**해결:**
1. Actions 탭에서 실패한 step 클릭
2. 로그 확인
3. 로컬에서 `./gradlew test` 실행해서 동일한 에러 재현
4. 수정 후 다시 push

### 문제 3: 환경 변수 누락

```
Error: DB_URL is not set
```

**해결:**
1. GitHub → Settings → Secrets → Actions
2. 필요한 환경 변수 추가

---

## 9. 유용한 표현식

```yaml
# 브랜치 이름
${{ github.ref }}              # refs/heads/feature/auth
${{ github.ref_name }}         # feature/auth

# 커밋 정보
${{ github.sha }}              # abc123...
${{ github.actor }}            # 커밋한 사람

# 조건부 실행
if: github.ref == 'refs/heads/main'     # main 브랜치만
if: success()                            # 이전 step 성공 시
if: failure()                            # 이전 step 실패 시
if: always()                             # 항상 실행
```

---

## 10. 다음 단계

이 튜토리얼에서 다룬 내용:
- [x] GitHub Actions 기본 개념
- [x] 워크플로우 파일 구조
- [x] 트리거 설정
- [x] 빌드/테스트 자동화
- [x] 팀 협업 워크플로우

추가로 학습할 내용:
- [ ] 캐싱으로 빌드 속도 향상
- [ ] Docker 이미지 빌드
- [ ] 자동 배포 (CD)
- [ ] 슬랙/디스코드 알림 연동
- [ ] 브랜치 보호 규칙 설정
