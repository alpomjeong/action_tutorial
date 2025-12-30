# Clean Architecture 튜토리얼

> Spring Boot에서 Clean Architecture를 쉽게 이해하고 적용하는 가이드

## 문서 정보

| 항목 | 내용 |
|------|------|
| **레벨** | 중급 |
| **예상 읽기 시간** | 45분 |
| **선행 지식** | Spring Boot 기초, 객체지향 프로그래밍 |
| **최종 업데이트** | 2025년 1월 |

### 관련 문서
- [INDEX.md](INDEX.md) - 전체 문서 가이드
- [SPRING_TEST_MODULARIZATION.md](SPRING_TEST_MODULARIZATION.md) - 테스트 모듈화

---

## 목차

1. [Clean Architecture란?](#1-clean-architecture란)
2. [왜 Clean Architecture인가?](#2-왜-clean-architecture인가)
3. [4개의 레이어 이해하기](#3-4개의-레이어-이해하기)
4. [핵심 원칙: 의존성 규칙](#4-핵심-원칙-의존성-규칙)
5. [Spring Boot 프로젝트 구조](#5-spring-boot-프로젝트-구조)
6. [실전 구현: User 도메인](#6-실전-구현-user-도메인)
7. [Port와 Adapter 패턴](#7-port와-adapter-패턴)
8. [테스트 전략](#8-테스트-전략)
9. [흔한 오해와 실수](#9-흔한-오해와-실수)
10. [점진적 도입 가이드](#10-점진적-도입-가이드)

---

## 1. Clean Architecture란?

### 비유로 이해하기

Clean Architecture를 **양파**에 비유하면 이해하기 쉽습니다.

```
          ┌─────────────────────────────────────┐
          │         외부 세계 (껍질)              │
          │    DB, Web, UI, Framework           │
          │   ┌─────────────────────────────┐   │
          │   │      Interface Adapters      │   │
          │   │   Controllers, Gateways      │   │
          │   │   ┌─────────────────────┐   │   │
          │   │   │     Use Cases       │   │   │
          │   │   │   비즈니스 로직      │   │   │
          │   │   │   ┌─────────────┐   │   │   │
          │   │   │   │  Entities   │   │   │   │
          │   │   │   │ 핵심 도메인  │   │   │   │
          │   │   │   └─────────────┘   │   │   │
          │   │   └─────────────────────┘   │   │
          │   └─────────────────────────────┘   │
          └─────────────────────────────────────┘
```

**핵심 아이디어**: 안쪽으로 갈수록 더 중요하고, 바깥쪽은 언제든 교체 가능해야 한다.

### 실생활 비유

레스토랑을 생각해보세요:

| 구성 요소 | 레스토랑 비유 | 소프트웨어 |
|----------|-------------|-----------|
| **Entities** | 요리 레시피 | 핵심 비즈니스 규칙 |
| **Use Cases** | 주문 처리 과정 | 애플리케이션 비즈니스 로직 |
| **Interface Adapters** | 웨이터, 주방 인터페이스 | Controller, Repository |
| **Frameworks** | 주방 기구, POS 시스템 | Spring, JPA, MySQL |

레시피(Entities)는 주방 기구가 바뀌어도 변하지 않습니다. 마찬가지로 비즈니스 규칙은 데이터베이스가 바뀌어도 변하면 안 됩니다.

---

## 2. 왜 Clean Architecture인가?

### 전통적인 계층형 아키텍처의 문제

```
┌──────────────────┐
│   Controller     │
└────────┬─────────┘
         │ 의존
         ▼
┌──────────────────┐
│    Service       │
└────────┬─────────┘
         │ 의존
         ▼
┌──────────────────┐
│   Repository     │  ← JPA에 강하게 결합
└────────┬─────────┘
         │ 의존
         ▼
┌──────────────────┐
│    Database      │
└──────────────────┘
```

**문제점:**
- Service가 JPA Entity에 직접 의존
- 데이터베이스 변경 시 전체 코드 수정 필요
- 테스트할 때 DB 필요

### Clean Architecture의 해결책

```
┌──────────────────┐
│   Controller     │  ← Framework Layer
└────────┬─────────┘
         │ 의존
         ▼
┌──────────────────┐
│    Use Case      │  ← Application Layer
│   (Interface)    │
└────────┬─────────┘
         │ 의존 (추상화)
         ▼
┌──────────────────┐
│     Entity       │  ← Domain Layer
└──────────────────┘
         ▲
         │ 구현 (역방향)
┌──────────────────┐
│   Repository     │  ← Infrastructure Layer
│  (구현체)         │
└──────────────────┘
```

**이점:**
- 비즈니스 로직이 프레임워크에 독립적
- 데이터베이스 교체가 쉬움
- 테스트가 간단함

---

## 3. 4개의 레이어 이해하기

### 3.1 Entities (도메인 계층)

가장 안쪽, 가장 중요한 계층입니다.

```java
// 순수 Java 객체 - 어떤 프레임워크에도 의존하지 않음
public class User {
    private Long id;
    private String email;
    private String name;
    private UserStatus status;

    // 비즈니스 규칙을 메서드로 표현
    public void activate() {
        if (this.status == UserStatus.BANNED) {
            throw new IllegalStateException("차단된 사용자는 활성화할 수 없습니다");
        }
        this.status = UserStatus.ACTIVE;
    }

    public boolean canPost() {
        return this.status == UserStatus.ACTIVE;
    }
}
```

**특징:**
- 순수 Java 객체 (POJO)
- 비즈니스 규칙 포함
- 어떤 프레임워크에도 의존하지 않음
- 가장 변경 빈도가 낮음

### 3.2 Use Cases (애플리케이션 계층)

비즈니스 시나리오를 구현하는 계층입니다.

```java
// Use Case 인터페이스 (Input Port)
public interface CreateUserUseCase {
    UserResponse execute(CreateUserCommand command);
}

// Use Case 구현
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;  // Output Port
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse execute(CreateUserCommand command) {
        // 1. 비즈니스 규칙 검증
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new EmailAlreadyExistsException(command.getEmail());
        }

        // 2. 도메인 객체 생성
        User user = User.create(
            command.getEmail(),
            passwordEncoder.encode(command.getPassword()),
            command.getName()
        );

        // 3. 저장 및 반환
        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }
}
```

**특징:**
- 애플리케이션의 "무엇을 할 것인가" 정의
- 도메인 객체들을 조합하여 시나리오 구현
- Input Port(인터페이스)와 Output Port(저장소 인터페이스) 정의

### 3.3 Interface Adapters (인터페이스 어댑터 계층)

외부 세계와 Use Case를 연결합니다.

```java
// Controller - 외부 요청을 Use Case 형식으로 변환
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        // HTTP 요청 → Use Case Command로 변환
        CreateUserCommand command = new CreateUserCommand(
            request.getEmail(),
            request.getPassword(),
            request.getName()
        );

        UserResponse response = createUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

// Repository 구현체 - Use Case의 Output Port 구현
@Repository
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.from(user);
        UserEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
```

**특징:**
- Controller: HTTP → Use Case 변환
- Repository 구현체: Use Case → DB 변환
- Presenter: Use Case 결과 → 응답 형식 변환

### 3.4 Frameworks & Drivers (프레임워크 계층)

가장 바깥쪽 계층입니다.

```java
// JPA Entity - 프레임워크 의존적
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    // 도메인 → Entity 변환
    public static UserEntity from(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getId();
        entity.email = user.getEmail();
        entity.name = user.getName();
        entity.status = user.getStatus();
        return entity;
    }

    // Entity → 도메인 변환
    public User toDomain() {
        return new User(id, email, name, status);
    }
}

// Spring Data JPA Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByEmail(String email);
    Optional<UserEntity> findByEmail(String email);
}
```

**특징:**
- 프레임워크 관련 코드 (JPA, Spring 등)
- 쉽게 교체 가능해야 함
- 안쪽 계층에 영향을 주지 않음

---

## 4. 핵심 원칙: 의존성 규칙

### The Dependency Rule

> **"소스 코드 의존성은 반드시 안쪽으로, 고수준 정책을 향해야 한다"**

```
┌─────────────────────────────────────────────────┐
│                  Frameworks                      │
│  ┌───────────────────────────────────────────┐  │
│  │            Interface Adapters              │  │
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │             Use Cases               │  │  │
│  │  │  ┌───────────────────────────────┐  │  │  │
│  │  │  │           Entities            │  │  │  │
│  │  │  └───────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                      │
                      │ 의존성 방향
                      ▼
                    안쪽으로
```

### 의존성 역전 (Dependency Inversion)

Use Case가 Repository를 직접 의존하면 안쪽에서 바깥쪽을 의존하게 됩니다.
이를 해결하기 위해 **인터페이스**를 사용합니다.

```
❌ 잘못된 방향:
UseCase → JpaUserRepository (바깥쪽 의존)

✅ 올바른 방향:
UseCase → UserRepository (Interface, 안쪽에 정의)
                ↑
JpaUserRepository (바깥쪽에서 구현)
```

```java
// domain 패키지에 정의 (안쪽)
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
}

// infrastructure 패키지에서 구현 (바깥쪽)
@Repository
public class JpaUserRepository implements UserRepository {
    // JPA를 사용한 구현
}
```

---

## 5. Spring Boot 프로젝트 구조

### 패키지 구조 (Feature 기반)

```
src/main/java/com/example/app/
├── user/                          # User 기능
│   ├── domain/                    # 도메인 계층
│   │   ├── User.java              # Entity
│   │   ├── UserRepository.java    # Repository Interface (Port)
│   │   └── UserStatus.java        # Value Object
│   │
│   ├── application/               # 애플리케이션 계층
│   │   ├── port/
│   │   │   ├── in/                # Input Ports
│   │   │   │   ├── CreateUserUseCase.java
│   │   │   │   └── GetUserUseCase.java
│   │   │   └── out/               # Output Ports (= Repository Interface)
│   │   │       └── LoadUserPort.java
│   │   ├── service/
│   │   │   └── CreateUserService.java
│   │   └── dto/
│   │       ├── CreateUserCommand.java
│   │       └── UserResponse.java
│   │
│   └── infrastructure/            # 인프라 계층
│       ├── persistence/
│       │   ├── UserEntity.java    # JPA Entity
│       │   ├── UserJpaRepository.java
│       │   └── UserPersistenceAdapter.java
│       └── web/
│           ├── UserController.java
│           └── UserRequest.java
│
├── board/                         # Board 기능 (동일 구조)
│   ├── domain/
│   ├── application/
│   └── infrastructure/
│
└── common/                        # 공통
    ├── exception/
    └── config/
```

### 간소화된 구조 (소규모 프로젝트)

```
src/main/java/com/example/app/
├── domain/                        # 도메인 계층
│   ├── user/
│   │   ├── User.java
│   │   └── UserRepository.java
│   └── board/
│       ├── Board.java
│       └── BoardRepository.java
│
├── application/                   # 애플리케이션 계층
│   ├── user/
│   │   ├── CreateUserUseCase.java
│   │   ├── CreateUserService.java
│   │   └── UserResponse.java
│   └── board/
│
└── infrastructure/                # 인프라 계층
    ├── persistence/
    │   ├── user/
    │   │   ├── UserEntity.java
    │   │   └── JpaUserRepository.java
    │   └── board/
    └── web/
        ├── user/
        │   └── UserController.java
        └── board/
```

---

## 6. 실전 구현: User 도메인

### 6.1 Domain 계층

```java
// domain/user/User.java
public class User {
    private Long id;
    private Email email;        // Value Object
    private String name;
    private Password password;  // Value Object
    private UserStatus status;
    private LocalDateTime createdAt;

    // 생성자 - 비즈니스 규칙 적용
    public static User create(String email, String password, String name) {
        User user = new User();
        user.email = Email.of(email);         // 이메일 형식 검증
        user.password = Password.of(password); // 비밀번호 규칙 검증
        user.name = name;
        user.status = UserStatus.PENDING;
        user.createdAt = LocalDateTime.now();
        return user;
    }

    // 비즈니스 로직
    public void activate() {
        if (this.status == UserStatus.BANNED) {
            throw new UserException("차단된 사용자는 활성화할 수 없습니다");
        }
        this.status = UserStatus.ACTIVE;
    }

    public void ban(String reason) {
        this.status = UserStatus.BANNED;
    }

    public boolean canCreatePost() {
        return this.status == UserStatus.ACTIVE;
    }

    // Getters...
}

// domain/user/Email.java (Value Object)
public class Email {
    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String value) {
        if (value == null || !value.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new InvalidEmailException(value);
        }
        return new Email(value);
    }

    public String getValue() {
        return value;
    }
}

// domain/user/UserRepository.java (Port)
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(Email email);
    boolean existsByEmail(Email email);
    void delete(User user);
}
```

### 6.2 Application 계층

```java
// application/user/port/in/CreateUserUseCase.java
public interface CreateUserUseCase {
    UserResponse execute(CreateUserCommand command);
}

// application/user/dto/CreateUserCommand.java
public record CreateUserCommand(
    String email,
    String password,
    String name
) {
    public CreateUserCommand {
        Objects.requireNonNull(email, "이메일은 필수입니다");
        Objects.requireNonNull(password, "비밀번호는 필수입니다");
        Objects.requireNonNull(name, "이름은 필수입니다");
    }
}

// application/user/dto/UserResponse.java
public record UserResponse(
    Long id,
    String email,
    String name,
    String status,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail().getValue(),
            user.getName(),
            user.getStatus().name(),
            user.getCreatedAt()
        );
    }
}

// application/user/service/CreateUserService.java
@Service
@Transactional
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserService(UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse execute(CreateUserCommand command) {
        // 1. 중복 검사
        Email email = Email.of(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(command.email());
        }

        // 2. 도메인 객체 생성
        User user = User.create(
            command.email(),
            passwordEncoder.encode(command.password()),
            command.name()
        );

        // 3. 저장
        User savedUser = userRepository.save(user);

        // 4. 응답 변환
        return UserResponse.from(savedUser);
    }
}
```

### 6.3 Infrastructure 계층

```java
// infrastructure/persistence/user/UserEntity.java
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Domain → Entity
    public static UserEntity from(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getId();
        entity.email = user.getEmail().getValue();
        entity.name = user.getName();
        entity.password = user.getPassword().getValue();
        entity.status = user.getStatus();
        entity.createdAt = user.getCreatedAt();
        return entity;
    }

    // Entity → Domain
    public User toDomain() {
        return User.reconstitute(
            id,
            Email.of(email),
            name,
            Password.ofEncoded(password),
            status,
            createdAt
        );
    }
}

// infrastructure/persistence/user/UserJpaRepository.java
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}

// infrastructure/persistence/user/UserPersistenceAdapter.java
@Repository
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserPersistenceAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.from(user);
        UserEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
            .map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue())
            .map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }

    @Override
    public void delete(User user) {
        jpaRepository.deleteById(user.getId());
    }
}

// infrastructure/web/user/UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase,
                         GetUserUseCase getUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserRequest request) {
        CreateUserCommand command = new CreateUserCommand(
            request.email(),
            request.password(),
            request.name()
        );
        UserResponse response = createUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse response = getUserUseCase.execute(id);
        return ResponseEntity.ok(response);
    }
}

// infrastructure/web/user/UserRequest.java
public record UserRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    String password,

    @NotBlank(message = "이름은 필수입니다")
    String name
) {}
```

---

## 7. Port와 Adapter 패턴

### 개념 이해

Clean Architecture에서 Port와 Adapter는 **육각형 아키텍처(Hexagonal Architecture)**의 개념입니다.

```
                    ┌─────────────────┐
                    │   Web Client    │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Web Adapter    │  ← Driving Adapter
                    │  (Controller)   │
                    └────────┬────────┘
                             │
              ┌──────────────▼──────────────┐
              │         Input Port          │
              │     (Use Case Interface)    │
              ├─────────────────────────────┤
              │                             │
              │      Application Core       │
              │    (Use Cases + Domain)     │
              │                             │
              ├─────────────────────────────┤
              │        Output Port          │
              │   (Repository Interface)    │
              └──────────────┬──────────────┘
                             │
                    ┌────────▼────────┐
                    │   DB Adapter    │  ← Driven Adapter
                    │ (JpaRepository) │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │    Database     │
                    └─────────────────┘
```

### Port 종류

#### Input Port (Driving Port)
외부에서 애플리케이션을 "호출"하는 인터페이스

```java
// 사용자 생성 Use Case - Input Port
public interface CreateUserUseCase {
    UserResponse execute(CreateUserCommand command);
}

// 사용자 조회 Use Case - Input Port
public interface GetUserUseCase {
    UserResponse execute(Long userId);
}

// 사용자 삭제 Use Case - Input Port
public interface DeleteUserUseCase {
    void execute(Long userId);
}
```

#### Output Port (Driven Port)
애플리케이션이 외부 시스템을 "호출"하는 인터페이스

```java
// 사용자 저장소 - Output Port
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
}

// 이메일 발송 - Output Port
public interface EmailSender {
    void send(String to, String subject, String body);
}

// 외부 API 호출 - Output Port
public interface PaymentGateway {
    PaymentResult process(PaymentRequest request);
}
```

### Adapter 종류

#### Driving Adapter (Primary Adapter)
외부에서 들어오는 요청을 처리

```java
// Web Adapter
@RestController
public class UserController {
    private final CreateUserUseCase createUserUseCase;
    // HTTP 요청 → Use Case 호출
}

// CLI Adapter
@Component
public class UserCommandLineRunner implements CommandLineRunner {
    private final CreateUserUseCase createUserUseCase;
    // 커맨드라인 → Use Case 호출
}

// Message Adapter
@Component
public class UserMessageListener {
    private final CreateUserUseCase createUserUseCase;
    // 메시지 큐 → Use Case 호출
}
```

#### Driven Adapter (Secondary Adapter)
애플리케이션이 외부 시스템을 호출할 때 사용

```java
// Database Adapter
@Repository
public class UserPersistenceAdapter implements UserRepository {
    private final UserJpaRepository jpaRepository;
    // Use Case → Database
}

// Email Adapter
@Component
public class SmtpEmailAdapter implements EmailSender {
    private final JavaMailSender mailSender;
    // Use Case → SMTP Server
}

// External API Adapter
@Component
public class StripePaymentAdapter implements PaymentGateway {
    private final StripeClient stripeClient;
    // Use Case → Stripe API
}
```

---

## 8. 테스트 전략

### 8.1 계층별 테스트

```
┌─────────────────────────────────────────────────────────┐
│  E2E Test (전체 흐름)                                    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Integration Test (Controller + Service + DB)      │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  Use Case Test (Service + Mock Repository)  │  │  │
│  │  │  ┌───────────────────────────────────────┐  │  │  │
│  │  │  │  Domain Test (Entity 단위 테스트)     │  │  │  │
│  │  │  └───────────────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 8.2 Domain 테스트 (가장 빠름)

```java
class UserTest {

    @Test
    void 사용자_생성_성공() {
        // given & when
        User user = User.create("test@example.com", "password123", "홍길동");

        // then
        assertThat(user.getEmail().getValue()).isEqualTo("test@example.com");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void 잘못된_이메일로_생성_실패() {
        assertThatThrownBy(() -> User.create("invalid-email", "password123", "홍길동"))
            .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void 차단된_사용자_활성화_실패() {
        // given
        User user = User.create("test@example.com", "password123", "홍길동");
        user.ban("규정 위반");

        // when & then
        assertThatThrownBy(user::activate)
            .isInstanceOf(UserException.class)
            .hasMessageContaining("차단된 사용자");
    }

    @Test
    void 활성_사용자만_게시물_작성_가능() {
        // given
        User activeUser = User.create("active@example.com", "password", "활성유저");
        activeUser.activate();

        User pendingUser = User.create("pending@example.com", "password", "대기유저");

        // then
        assertThat(activeUser.canCreatePost()).isTrue();
        assertThat(pendingUser.canCreatePost()).isFalse();
    }
}
```

### 8.3 Use Case 테스트 (Mock 사용)

```java
@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateUserService createUserService;

    @Test
    void 사용자_생성_성공() {
        // given
        CreateUserCommand command = new CreateUserCommand(
            "test@example.com",
            "password123",
            "홍길동"
        );

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // ID 설정된 User 반환 시뮬레이션
            return User.reconstitute(1L, user.getEmail(), user.getName(),
                user.getPassword(), user.getStatus(), user.getCreatedAt());
        });

        // when
        UserResponse response = createUserService.execute(command);

        // then
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 중복_이메일로_생성_실패() {
        // given
        CreateUserCommand command = new CreateUserCommand(
            "existing@example.com",
            "password123",
            "홍길동"
        );

        when(userRepository.existsByEmail(any())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> createUserService.execute(command))
            .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }
}
```

### 8.4 In-Memory 구현체로 테스트

Mock 대신 In-Memory 구현체를 사용하면 더 실제와 가까운 테스트가 가능합니다.

```java
// 테스트용 In-Memory Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            Long newId = idGenerator.incrementAndGet();
            User newUser = User.reconstitute(
                newId, user.getEmail(), user.getName(),
                user.getPassword(), user.getStatus(), user.getCreatedAt()
            );
            store.put(newId, newUser);
            return newUser;
        }
        store.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return store.values().stream()
            .filter(u -> u.getEmail().equals(email))
            .findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public void delete(User user) {
        store.remove(user.getId());
    }

    // 테스트 헬퍼 메서드
    public void clear() {
        store.clear();
        idGenerator.set(0);
    }
}

// In-Memory Repository를 사용한 테스트
class CreateUserServiceIntegrationTest {

    private InMemoryUserRepository userRepository;
    private CreateUserService createUserService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        PasswordEncoder encoder = password -> "encoded_" + password;
        createUserService = new CreateUserService(userRepository, encoder);
    }

    @Test
    void 사용자_생성_후_조회() {
        // given
        CreateUserCommand command = new CreateUserCommand(
            "test@example.com", "password123", "홍길동"
        );

        // when
        UserResponse created = createUserService.execute(command);

        // then
        Optional<User> found = userRepository.findById(created.id());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("홍길동");
    }
}
```

### 8.5 Controller 테스트

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateUserUseCase createUserUseCase;

    @MockBean
    private GetUserUseCase getUserUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 사용자_생성_API_성공() throws Exception {
        // given
        UserRequest request = new UserRequest(
            "test@example.com",
            "password123",
            "홍길동"
        );

        UserResponse response = new UserResponse(
            1L, "test@example.com", "홍길동", "PENDING", LocalDateTime.now()
        );

        when(createUserUseCase.execute(any())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("홍길동"));
    }

    @Test
    void 잘못된_요청_검증_실패() throws Exception {
        // given
        UserRequest request = new UserRequest(
            "invalid-email",  // 잘못된 이메일
            "123",           // 너무 짧은 비밀번호
            ""               // 빈 이름
        );

        // when & then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

---

## 9. 흔한 오해와 실수

### 오해 1: "폴더만 나누면 Clean Architecture다"

```
❌ 폴더만 나눈 경우:
src/
├── domain/
│   └── User.java          // @Entity 어노테이션 포함
├── application/
│   └── UserService.java   // JpaRepository 직접 의존
└── infrastructure/
    └── UserController.java

→ 폴더 구조는 맞지만 의존성 방향이 잘못됨
```

```
✅ 올바른 경우:
src/
├── domain/
│   ├── User.java          // 순수 Java 객체
│   └── UserRepository.java // 인터페이스
├── application/
│   └── UserService.java   // UserRepository 인터페이스에 의존
└── infrastructure/
    ├── UserEntity.java    // @Entity 여기에
    └── JpaUserRepository.java // 인터페이스 구현
```

### 오해 2: "항상 모든 계층이 필요하다"

```
❌ 과도한 추상화:
// 단순 CRUD인데 4개 계층 전부 만듦
CreateUserUseCase (interface)
CreateUserService (구현)
CreateUserCommand (입력)
CreateUserResponse (출력)
User (도메인)
UserEntity (영속성)
UserRepository (인터페이스)
JpaUserRepository (구현)

→ 코드량만 늘고 생산성 저하
```

```
✅ 적절한 수준:
// 단순 CRUD는 간단하게
UserService → UserRepository (interface) → JpaUserRepository

// 복잡한 비즈니스 로직이 있을 때만 Use Case 분리
ComplexBusinessUseCase → 여러 도메인 서비스 조합
```

### 오해 3: "JPA Entity를 도메인으로 써도 된다"

```java
// ❌ JPA Entity를 도메인으로 사용
@Entity
public class User {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Post> posts;  // JPA 관계

    // 비즈니스 로직과 JPA가 혼재
    @Transient
    public boolean canPost() { ... }
}

// Service에서 직접 사용
User user = userRepository.findById(id);  // JPA Entity
user.getPosts();  // Lazy Loading 발생 가능
```

```java
// ✅ 분리된 구조
// Domain
public class User {
    private Long id;
    private Email email;
    // 순수 비즈니스 로직만
    public boolean canPost() { ... }
}

// Infrastructure
@Entity
public class UserEntity {
    @Id @GeneratedValue
    private Long id;

    public User toDomain() {
        return new User(id, Email.of(email), ...);
    }
}
```

### 오해 4: "의존성 주입만 하면 된다"

```java
// ❌ DI만 하고 인터페이스 없이 직접 의존
@Service
public class UserService {
    private final JpaUserRepository repository;  // 구현체에 직접 의존

    // JPA 변경 시 Service도 수정 필요
}
```

```java
// ✅ 인터페이스를 통한 의존성 역전
@Service
public class UserService {
    private final UserRepository repository;  // 인터페이스에 의존

    // 구현체가 바뀌어도 Service 코드 변경 불필요
}
```

### 실수 1: 순환 의존성

```java
// ❌ 순환 의존성 발생
public class OrderService {
    private final UserService userService;  // Order → User
}

public class UserService {
    private final OrderService orderService;  // User → Order (순환!)
}
```

```java
// ✅ 이벤트 또는 인터페이스로 해결
public class OrderService {
    private final UserQueryPort userQueryPort;  // 인터페이스 의존
}

// 또는 도메인 이벤트 사용
public class UserService {
    private final ApplicationEventPublisher eventPublisher;

    public void deleteUser(Long id) {
        // ...
        eventPublisher.publishEvent(new UserDeletedEvent(id));
    }
}

@Component
public class OrderEventHandler {
    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) {
        // 관련 주문 처리
    }
}
```

---

## 10. 점진적 도입 가이드

### 단계 1: 기존 프로젝트 분석

```
현재 구조 파악:
├── Controller → Service → Repository → Entity
└── 의존성: 모두 아래로 향함 (OK)
    문제점: Service가 JPA Entity에 직접 의존
```

### 단계 2: Domain 분리 (가장 먼저)

```java
// 1. 순수 도메인 객체 생성
public class User {
    private Long id;
    private String email;
    // JPA 어노테이션 없음
}

// 2. 기존 Entity는 유지하되 변환 메서드 추가
@Entity
public class UserEntity {
    // ... JPA 매핑

    public User toDomain() { ... }
    public static UserEntity from(User user) { ... }
}
```

### 단계 3: Repository 인터페이스 추출

```java
// 1. 인터페이스 생성 (domain 패키지)
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
}

// 2. 구현체 생성 (infrastructure 패키지)
@Repository
public class JpaUserRepository implements UserRepository {
    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        return jpaRepository.save(UserEntity.from(user)).toDomain();
    }
}

// 3. Service 수정
@Service
public class UserService {
    private final UserRepository userRepository;  // 인터페이스로 변경
}
```

### 단계 4: Use Case 분리 (필요할 때만)

```java
// 복잡한 비즈니스 로직이 있을 때만
public interface ComplexUserOperationUseCase {
    Result execute(Command command);
}

// 단순 CRUD는 그냥 Service로 유지해도 됨
```

### 단계별 체크리스트

```
□ Phase 1: 도메인 분리
  □ 순수 도메인 객체 생성
  □ JPA Entity에 변환 메서드 추가
  □ Value Object 식별 및 생성

□ Phase 2: Repository 추상화
  □ Repository 인터페이스 생성
  □ JPA Repository 구현체 생성
  □ Service에서 인터페이스 사용

□ Phase 3: Use Case 분리 (선택)
  □ 복잡한 비즈니스 로직 식별
  □ Use Case 인터페이스 생성
  □ Service를 Use Case 구현으로 변경

□ Phase 4: 테스트 정비
  □ 도메인 단위 테스트 추가
  □ Use Case 테스트 (Mock/In-Memory)
  □ 통합 테스트 유지
```

---

## 요약 정리

### Clean Architecture 핵심 3가지

```
1. 의존성 규칙: 안쪽으로만 의존
   Frameworks → Adapters → Use Cases → Entities

2. 의존성 역전: 인터페이스로 방향 바꾸기
   Service → Repository(Interface) ← JpaRepository(구현)

3. 관심사 분리: 각 계층은 하나의 역할만
   Domain: 비즈니스 규칙
   Application: 비즈니스 시나리오
   Infrastructure: 외부 연결
```

### 언제 적용해야 하나?

| 프로젝트 규모 | 권장 수준 |
|-------------|----------|
| 개인/학습 | 전통적 계층형으로 충분 |
| 소규모 팀 | Domain 분리 정도만 |
| 중규모 팀 | Repository 추상화까지 |
| 대규모/MSA | Full Clean Architecture |

### 적용 우선순위

```
1순위: Domain 객체 분리 (비용 낮음, 효과 높음)
2순위: Repository 인터페이스 추출 (테스트 용이성)
3순위: Use Case 분리 (복잡한 비즈니스 로직이 있을 때)
4순위: Port/Adapter 완전 분리 (대규모 시스템)
```

---

## 다음 단계

- [테스트 모듈화 전략](SPRING_TEST_MODULARIZATION.md)
- [GitHub Actions CI/CD](GITHUB_ACTIONS_TUTORIAL.md)
- [전체 문서 가이드](INDEX.md)

---

## 참고 자료

- Robert C. Martin, "Clean Architecture" (2017)
- Tom Hombergs, "Get Your Hands Dirty on Clean Architecture" (2019)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
