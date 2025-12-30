# 알림 기능 아키텍처 가이드

> 커뮤니티 앱에서 알림 기능을 효율적으로 구현하는 방법

## 문서 정보

| 항목 | 내용 |
|------|------|
| **레벨** | 중급 |
| **예상 읽기 시간** | 30분 |
| **선행 지식** | Spring Boot, Clean Architecture 기초 |
| **최종 업데이트** | 2025년 1월 |

### 관련 문서
- [CLEAN_ARCHITECTURE_TUTORIAL.md](CLEAN_ARCHITECTURE_TUTORIAL.md) - Clean Architecture 기초
- [SPRING_TEST_MODULARIZATION.md](SPRING_TEST_MODULARIZATION.md) - 테스트 전략

---

## 목차

1. [알림 시스템 개요](#1-알림-시스템-개요)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [Domain 계층 구현](#3-domain-계층-구현)
4. [Application 계층 구현](#4-application-계층-구현)
5. [Infrastructure 계층 구현](#5-infrastructure-계층-구현)
6. [이벤트 기반 알림 발송](#6-이벤트-기반-알림-발송)
7. [비동기 처리 설정](#7-비동기-처리-설정)
8. [알림 타입별 구현](#8-알림-타입별-구현)
9. [테스트 전략](#9-테스트-전략)
10. [단계별 확장 가이드](#10-단계별-확장-가이드)

---

## 1. 알림 시스템 개요

### 커뮤니티 앱의 알림 시나리오

```
┌─────────────────────────────────────────────────────────────┐
│                    알림이 발생하는 상황                       │
├─────────────────────────────────────────────────────────────┤
│  1. 내 글에 댓글이 달렸을 때                                  │
│  2. 내 댓글에 대댓글이 달렸을 때                              │
│  3. 누군가 나를 팔로우했을 때                                 │
│  4. 팔로우한 사람이 새 글을 작성했을 때                        │
│  5. 내 글에 좋아요가 눌렸을 때                                │
└─────────────────────────────────────────────────────────────┘
```

### 아키텍처 선택: 서버 분리 vs 단일 서버

```
❌ 서버 분리 (소규모에서 비추천)
────────────────────────────────
┌──────────┐    ┌──────────┐    ┌──────────────────┐
│ API 서버 │ → │ 메시지 큐 │ → │ 알림 서버 (별도) │
└──────────┘    └──────────┘    └──────────────────┘
     💰             💰                  💰
  비용 증가      운영 복잡도         추가 서버 비용


✅ 단일 서버 + 비동기 (추천)
────────────────────────────────
┌─────────────────────────────────────────┐
│              API 서버 (1대)              │
│  ┌─────────────┐    ┌────────────────┐  │
│  │  API 요청   │    │  @Async 스레드  │  │
│  │  처리       │ →  │  알림 발송      │  │
│  │  (메인)     │    │  (백그라운드)   │  │
│  └─────────────┘    └────────────────┘  │
└─────────────────────────────────────────┘
     💰 추가 비용 없음
```

### 동기 vs 비동기 처리 비교

```
동기 처리 (사용자 경험 나쁨)
────────────────────────────
[요청] → [DB 저장] → [알림 1000건 발송] → [응답]
                      ↑ 10초 대기
총 응답: 10초+ 😱


비동기 처리 (사용자 경험 좋음)
────────────────────────────
[요청] → [DB 저장] → [응답]  ← 0.1초
              ↓
         [이벤트 발행]
              ↓
         [@Async 스레드에서 알림 발송]  ← 백그라운드
```

---

## 2. 프로젝트 구조

### 전체 패키지 구조

```
src/main/java/com/example/community/
│
├── user/
│   ├── domain/
│   │   ├── User.java
│   │   ├── UserRepository.java
│   │   └── Follow.java
│   ├── application/
│   │   └── UserService.java
│   └── infrastructure/
│       ├── persistence/
│       └── web/
│
├── board/
│   ├── domain/
│   │   ├── Board.java
│   │   └── BoardRepository.java
│   ├── application/
│   │   └── BoardService.java
│   └── infrastructure/
│
├── comment/
│   ├── domain/
│   │   ├── Comment.java
│   │   └── CommentRepository.java
│   ├── application/
│   │   └── CommentService.java
│   └── infrastructure/
│
├── notification/                    # 알림 도메인
│   ├── domain/
│   │   ├── Notification.java        # 알림 엔티티
│   │   ├── NotificationType.java    # 알림 타입 enum
│   │   └── NotificationRepository.java
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── SendNotificationUseCase.java
│   │   │   │   └── GetNotificationsUseCase.java
│   │   │   └── out/
│   │   │       └── NotificationSender.java    # 외부 발송 인터페이스
│   │   ├── service/
│   │   │   └── NotificationService.java
│   │   └── dto/
│   │       ├── NotificationCommand.java
│   │       └── NotificationResponse.java
│   └── infrastructure/
│       ├── persistence/
│       │   ├── NotificationEntity.java
│       │   ├── NotificationJpaRepository.java
│       │   └── NotificationPersistenceAdapter.java
│       ├── web/
│       │   └── NotificationController.java
│       └── external/
│           ├── FcmNotificationSender.java     # FCM 구현체
│           └── EmailNotificationSender.java   # 이메일 구현체
│
├── common/
│   ├── event/                       # 도메인 이벤트
│   │   ├── DomainEvent.java
│   │   ├── BoardCreatedEvent.java
│   │   ├── CommentCreatedEvent.java
│   │   └── FollowCreatedEvent.java
│   ├── config/
│   │   └── AsyncConfig.java         # 비동기 설정
│   └── exception/
│
└── CommunityApplication.java
```

---

## 3. Domain 계층 구현

### 3.1 Notification 엔티티

```java
// notification/domain/Notification.java
public class Notification {
    private Long id;
    private Long receiverId;          // 알림 받는 사람
    private Long senderId;            // 알림 발생시킨 사람 (nullable)
    private NotificationType type;    // 알림 타입
    private String title;
    private String content;
    private String targetUrl;         // 클릭 시 이동할 URL
    private Long targetId;            // 관련 엔티티 ID (게시글, 댓글 등)
    private boolean isRead;
    private LocalDateTime createdAt;

    // 생성 메서드
    public static Notification create(
            Long receiverId,
            Long senderId,
            NotificationType type,
            String title,
            String content,
            String targetUrl,
            Long targetId) {

        Notification notification = new Notification();
        notification.receiverId = receiverId;
        notification.senderId = senderId;
        notification.type = type;
        notification.title = title;
        notification.content = content;
        notification.targetUrl = targetUrl;
        notification.targetId = targetId;
        notification.isRead = false;
        notification.createdAt = LocalDateTime.now();
        return notification;
    }

    // 비즈니스 로직
    public void markAsRead() {
        this.isRead = true;
    }

    public boolean isUnread() {
        return !this.isRead;
    }

    // Getters...
}
```

### 3.2 NotificationType Enum

```java
// notification/domain/NotificationType.java
public enum NotificationType {
    // 댓글 관련
    COMMENT_ON_MY_BOARD("내 글에 댓글", "%s님이 회원님의 글에 댓글을 남겼습니다."),
    REPLY_ON_MY_COMMENT("내 댓글에 대댓글", "%s님이 회원님의 댓글에 답글을 남겼습니다."),

    // 팔로우 관련
    NEW_FOLLOWER("새 팔로워", "%s님이 회원님을 팔로우하기 시작했습니다."),
    FOLLOWING_NEW_BOARD("팔로우 새 글", "%s님이 새 글을 작성했습니다."),

    // 좋아요 관련
    LIKE_ON_MY_BOARD("내 글에 좋아요", "%s님이 회원님의 글을 좋아합니다."),

    // 시스템
    SYSTEM_NOTICE("시스템 공지", "%s");

    private final String displayName;
    private final String messageTemplate;

    NotificationType(String displayName, String messageTemplate) {
        this.displayName = displayName;
        this.messageTemplate = messageTemplate;
    }

    public String formatMessage(String senderName) {
        return String.format(messageTemplate, senderName);
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### 3.3 NotificationRepository 인터페이스

```java
// notification/domain/NotificationRepository.java
public interface NotificationRepository {
    Notification save(Notification notification);

    List<Notification> saveAll(List<Notification> notifications);

    Optional<Notification> findById(Long id);

    List<Notification> findByReceiverId(Long receiverId);

    List<Notification> findByReceiverIdAndIsReadFalse(Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    void markAllAsRead(Long receiverId);
}
```

---

## 4. Application 계층 구현

### 4.1 Use Case 인터페이스

```java
// notification/application/port/in/SendNotificationUseCase.java
public interface SendNotificationUseCase {
    void send(NotificationCommand command);
    void sendToMultiple(List<NotificationCommand> commands);
}

// notification/application/port/in/GetNotificationsUseCase.java
public interface GetNotificationsUseCase {
    List<NotificationResponse> getMyNotifications(Long userId);
    List<NotificationResponse> getUnreadNotifications(Long userId);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
}
```

### 4.2 Output Port (외부 발송 인터페이스)

```java
// notification/application/port/out/NotificationSender.java
public interface NotificationSender {
    void send(PushNotification notification);
    void sendBatch(List<PushNotification> notifications);
}

// notification/application/dto/PushNotification.java
public record PushNotification(
    String token,           // FCM 토큰 또는 이메일 주소
    String title,
    String body,
    Map<String, String> data
) {}
```

### 4.3 Command & Response DTO

```java
// notification/application/dto/NotificationCommand.java
public record NotificationCommand(
    Long receiverId,
    Long senderId,
    NotificationType type,
    String title,
    String content,
    String targetUrl,
    Long targetId
) {
    // 팩토리 메서드들
    public static NotificationCommand forNewComment(
            Long receiverId, Long senderId, String senderName,
            Long boardId, String boardTitle) {
        return new NotificationCommand(
            receiverId,
            senderId,
            NotificationType.COMMENT_ON_MY_BOARD,
            "새 댓글",
            NotificationType.COMMENT_ON_MY_BOARD.formatMessage(senderName),
            "/boards/" + boardId,
            boardId
        );
    }

    public static NotificationCommand forNewFollower(
            Long receiverId, Long senderId, String senderName) {
        return new NotificationCommand(
            receiverId,
            senderId,
            NotificationType.NEW_FOLLOWER,
            "새 팔로워",
            NotificationType.NEW_FOLLOWER.formatMessage(senderName),
            "/users/" + senderId,
            senderId
        );
    }

    public static NotificationCommand forFollowingNewBoard(
            Long receiverId, Long authorId, String authorName,
            Long boardId, String boardTitle) {
        return new NotificationCommand(
            receiverId,
            authorId,
            NotificationType.FOLLOWING_NEW_BOARD,
            boardTitle,
            NotificationType.FOLLOWING_NEW_BOARD.formatMessage(authorName),
            "/boards/" + boardId,
            boardId
        );
    }
}

// notification/application/dto/NotificationResponse.java
public record NotificationResponse(
    Long id,
    String type,
    String title,
    String content,
    String targetUrl,
    boolean isRead,
    LocalDateTime createdAt,
    String senderName,
    String senderProfileImage
) {
    public static NotificationResponse from(Notification notification, User sender) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType().name(),
            notification.getTitle(),
            notification.getContent(),
            notification.getTargetUrl(),
            notification.isRead(),
            notification.getCreatedAt(),
            sender != null ? sender.getName() : null,
            sender != null ? sender.getProfileImage() : null
        );
    }
}
```

### 4.4 NotificationService 구현

```java
// notification/application/service/NotificationService.java
@Service
@Transactional
public class NotificationService
        implements SendNotificationUseCase, GetNotificationsUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationSender notificationSender,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
        this.userRepository = userRepository;
    }

    @Override
    @Async("notificationExecutor")  // 비동기 실행
    public void send(NotificationCommand command) {
        // 1. DB에 알림 저장
        Notification notification = Notification.create(
            command.receiverId(),
            command.senderId(),
            command.type(),
            command.title(),
            command.content(),
            command.targetUrl(),
            command.targetId()
        );
        notificationRepository.save(notification);

        // 2. 푸시 알림 발송
        User receiver = userRepository.findById(command.receiverId())
            .orElse(null);

        if (receiver != null && receiver.getFcmToken() != null) {
            PushNotification push = new PushNotification(
                receiver.getFcmToken(),
                command.title(),
                command.content(),
                Map.of(
                    "type", command.type().name(),
                    "targetUrl", command.targetUrl()
                )
            );
            notificationSender.send(push);
        }
    }

    @Override
    @Async("notificationExecutor")
    public void sendToMultiple(List<NotificationCommand> commands) {
        // 1. DB에 일괄 저장
        List<Notification> notifications = commands.stream()
            .map(cmd -> Notification.create(
                cmd.receiverId(),
                cmd.senderId(),
                cmd.type(),
                cmd.title(),
                cmd.content(),
                cmd.targetUrl(),
                cmd.targetId()
            ))
            .toList();
        notificationRepository.saveAll(notifications);

        // 2. 푸시 알림 일괄 발송
        List<Long> receiverIds = commands.stream()
            .map(NotificationCommand::receiverId)
            .distinct()
            .toList();

        Map<Long, User> userMap = userRepository.findAllById(receiverIds)
            .stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        List<PushNotification> pushList = commands.stream()
            .map(cmd -> {
                User receiver = userMap.get(cmd.receiverId());
                if (receiver == null || receiver.getFcmToken() == null) {
                    return null;
                }
                return new PushNotification(
                    receiver.getFcmToken(),
                    cmd.title(),
                    cmd.content(),
                    Map.of("type", cmd.type().name(), "targetUrl", cmd.targetUrl())
                );
            })
            .filter(Objects::nonNull)
            .toList();

        if (!pushList.isEmpty()) {
            notificationSender.sendBatch(pushList);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
            .findByReceiverId(userId);
        return toResponseList(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
            .findByReceiverIdAndIsReadFalse(userId);
        return toResponseList(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getReceiverId().equals(userId)) {
            throw new UnauthorizedException("본인의 알림만 읽음 처리할 수 있습니다.");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private List<NotificationResponse> toResponseList(List<Notification> notifications) {
        List<Long> senderIds = notifications.stream()
            .map(Notification::getSenderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<Long, User> senderMap = userRepository.findAllById(senderIds)
            .stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        return notifications.stream()
            .map(n -> NotificationResponse.from(n, senderMap.get(n.getSenderId())))
            .toList();
    }
}
```

---

## 5. Infrastructure 계층 구현

### 5.1 JPA Entity

```java
// notification/infrastructure/persistence/NotificationEntity.java
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_receiver", columnList = "receiverId"),
    @Index(name = "idx_notification_receiver_unread", columnList = "receiverId, isRead")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long receiverId;

    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    private String targetUrl;

    private Long targetId;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static NotificationEntity from(Notification notification) {
        NotificationEntity entity = new NotificationEntity();
        entity.id = notification.getId();
        entity.receiverId = notification.getReceiverId();
        entity.senderId = notification.getSenderId();
        entity.type = notification.getType();
        entity.title = notification.getTitle();
        entity.content = notification.getContent();
        entity.targetUrl = notification.getTargetUrl();
        entity.targetId = notification.getTargetId();
        entity.isRead = notification.isRead();
        entity.createdAt = notification.getCreatedAt();
        return entity;
    }

    public Notification toDomain() {
        return Notification.reconstitute(
            id, receiverId, senderId, type, title,
            content, targetUrl, targetId, isRead, createdAt
        );
    }
}
```

### 5.2 JPA Repository

```java
// notification/infrastructure/persistence/NotificationJpaRepository.java
public interface NotificationJpaRepository
        extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    List<NotificationEntity> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(
        Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true " +
           "WHERE n.receiverId = :receiverId AND n.isRead = false")
    void markAllAsReadByReceiverId(@Param("receiverId") Long receiverId);
}
```

### 5.3 Persistence Adapter

```java
// notification/infrastructure/persistence/NotificationPersistenceAdapter.java
@Repository
public class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    public NotificationPersistenceAdapter(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationEntity.from(notification);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        List<NotificationEntity> entities = notifications.stream()
            .map(NotificationEntity::from)
            .toList();
        return jpaRepository.saveAll(entities).stream()
            .map(NotificationEntity::toDomain)
            .toList();
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id)
            .map(NotificationEntity::toDomain);
    }

    @Override
    public List<Notification> findByReceiverId(Long receiverId) {
        return jpaRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId)
            .stream()
            .map(NotificationEntity::toDomain)
            .toList();
    }

    @Override
    public List<Notification> findByReceiverIdAndIsReadFalse(Long receiverId) {
        return jpaRepository
            .findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(receiverId)
            .stream()
            .map(NotificationEntity::toDomain)
            .toList();
    }

    @Override
    public long countByReceiverIdAndIsReadFalse(Long receiverId) {
        return jpaRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Override
    public void markAllAsRead(Long receiverId) {
        jpaRepository.markAllAsReadByReceiverId(receiverId);
    }
}
```

### 5.4 FCM Notification Sender

```java
// notification/infrastructure/external/FcmNotificationSender.java
@Component
@Slf4j
public class FcmNotificationSender implements NotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    public FcmNotificationSender(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public void send(PushNotification notification) {
        try {
            Message message = Message.builder()
                .setToken(notification.token())
                .setNotification(Notification.builder()
                    .setTitle(notification.title())
                    .setBody(notification.body())
                    .build())
                .putAllData(notification.data())
                .build();

            firebaseMessaging.send(message);
            log.info("FCM 발송 성공: {}", notification.token());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패: {}", e.getMessage());
        }
    }

    @Override
    public void sendBatch(List<PushNotification> notifications) {
        List<Message> messages = notifications.stream()
            .map(n -> Message.builder()
                .setToken(n.token())
                .setNotification(Notification.builder()
                    .setTitle(n.title())
                    .setBody(n.body())
                    .build())
                .putAllData(n.data())
                .build())
            .toList();

        try {
            BatchResponse response = firebaseMessaging.sendAll(messages);
            log.info("FCM 일괄 발송: 성공 {}, 실패 {}",
                response.getSuccessCount(),
                response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 일괄 발송 실패: {}", e.getMessage());
        }
    }
}

// 개발환경용 Mock 구현체
@Component
@Profile("local")
@Primary
@Slf4j
public class MockNotificationSender implements NotificationSender {

    @Override
    public void send(PushNotification notification) {
        log.info("[MOCK] 알림 발송: {} - {}", notification.title(), notification.body());
    }

    @Override
    public void sendBatch(List<PushNotification> notifications) {
        log.info("[MOCK] 알림 일괄 발송: {}건", notifications.size());
        notifications.forEach(n ->
            log.info("  → {} - {}", n.title(), n.body()));
    }
}
```

### 5.5 Controller

```java
// notification/infrastructure/web/NotificationController.java
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;

    public NotificationController(GetNotificationsUseCase getNotificationsUseCase) {
        this.getNotificationsUseCase = getNotificationsUseCase;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(
            getNotificationsUseCase.getMyNotifications(userId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(
            getNotificationsUseCase.getUnreadNotifications(userId));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal Long userId) {
        long count = getNotificationsUseCase.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        getNotificationsUseCase.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal Long userId) {
        getNotificationsUseCase.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
```

---

## 6. 이벤트 기반 알림 발송

### 6.1 도메인 이벤트 정의

```java
// common/event/DomainEvent.java
public abstract class DomainEvent {
    private final LocalDateTime occurredAt;

    protected DomainEvent() {
        this.occurredAt = LocalDateTime.now();
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}

// common/event/BoardCreatedEvent.java
public class BoardCreatedEvent extends DomainEvent {
    private final Long boardId;
    private final Long authorId;
    private final String authorName;
    private final String boardTitle;

    public BoardCreatedEvent(Long boardId, Long authorId,
                            String authorName, String boardTitle) {
        super();
        this.boardId = boardId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.boardTitle = boardTitle;
    }

    // Getters...
}

// common/event/CommentCreatedEvent.java
public class CommentCreatedEvent extends DomainEvent {
    private final Long commentId;
    private final Long boardId;
    private final Long boardAuthorId;
    private final Long commentAuthorId;
    private final String commentAuthorName;
    private final String boardTitle;
    private final Long parentCommentId;        // 대댓글인 경우
    private final Long parentCommentAuthorId;  // 대댓글인 경우

    // Constructor, Getters...
}

// common/event/FollowCreatedEvent.java
public class FollowCreatedEvent extends DomainEvent {
    private final Long followerId;
    private final String followerName;
    private final Long followingId;

    // Constructor, Getters...
}
```

### 6.2 이벤트 발행 (Service에서)

```java
// board/application/service/BoardService.java
@Service
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BoardResponse create(CreateBoardCommand command, Long userId) {
        User author = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        Board board = Board.create(
            command.title(),
            command.content(),
            author.getId()
        );
        Board savedBoard = boardRepository.save(board);

        // 이벤트 발행 (비동기 처리는 리스너에서)
        eventPublisher.publishEvent(new BoardCreatedEvent(
            savedBoard.getId(),
            author.getId(),
            author.getName(),
            savedBoard.getTitle()
        ));

        return BoardResponse.from(savedBoard);
    }
}

// comment/application/service/CommentService.java
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CommentResponse create(CreateCommentCommand command, Long userId) {
        Board board = boardRepository.findById(command.boardId())
            .orElseThrow(() -> new BoardNotFoundException(command.boardId()));

        User author = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        Comment comment = Comment.create(
            command.content(),
            board.getId(),
            author.getId(),
            command.parentCommentId()
        );
        Comment savedComment = commentRepository.save(comment);

        // 이벤트 발행
        Long parentAuthorId = null;
        if (command.parentCommentId() != null) {
            Comment parentComment = commentRepository
                .findById(command.parentCommentId()).orElse(null);
            if (parentComment != null) {
                parentAuthorId = parentComment.getAuthorId();
            }
        }

        eventPublisher.publishEvent(new CommentCreatedEvent(
            savedComment.getId(),
            board.getId(),
            board.getAuthorId(),
            author.getId(),
            author.getName(),
            board.getTitle(),
            command.parentCommentId(),
            parentAuthorId
        ));

        return CommentResponse.from(savedComment);
    }
}
```

### 6.3 이벤트 리스너 (알림 발송)

```java
// notification/application/event/NotificationEventListener.java
@Component
@Slf4j
public class NotificationEventListener {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public NotificationEventListener(
            SendNotificationUseCase sendNotificationUseCase,
            UserRepository userRepository,
            FollowRepository followRepository) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    /**
     * 새 게시글 작성 시 → 팔로워들에게 알림
     */
    @Async("notificationExecutor")
    @EventListener
    public void handleBoardCreated(BoardCreatedEvent event) {
        log.info("BoardCreatedEvent 수신: boardId={}", event.getBoardId());

        // 작성자의 팔로워 목록 조회
        List<Long> followerIds = followRepository
            .findFollowerIdsByFollowingId(event.getAuthorId());

        if (followerIds.isEmpty()) {
            return;
        }

        // 알림 생성
        List<NotificationCommand> commands = followerIds.stream()
            .map(followerId -> NotificationCommand.forFollowingNewBoard(
                followerId,
                event.getAuthorId(),
                event.getAuthorName(),
                event.getBoardId(),
                event.getBoardTitle()
            ))
            .toList();

        sendNotificationUseCase.sendToMultiple(commands);
        log.info("팔로워 {}명에게 알림 발송 완료", commands.size());
    }

    /**
     * 댓글 작성 시 → 게시글 작성자에게 알림
     * 대댓글인 경우 → 부모 댓글 작성자에게도 알림
     */
    @Async("notificationExecutor")
    @EventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("CommentCreatedEvent 수신: commentId={}", event.getCommentId());

        // 자기 글에 자기가 댓글 단 경우 제외
        if (event.getCommentAuthorId().equals(event.getBoardAuthorId())) {
            log.info("본인 글에 본인 댓글 - 알림 생략");
        } else {
            // 게시글 작성자에게 알림
            NotificationCommand command = NotificationCommand.forNewComment(
                event.getBoardAuthorId(),
                event.getCommentAuthorId(),
                event.getCommentAuthorName(),
                event.getBoardId(),
                event.getBoardTitle()
            );
            sendNotificationUseCase.send(command);
        }

        // 대댓글인 경우, 부모 댓글 작성자에게도 알림
        if (event.getParentCommentId() != null
                && event.getParentCommentAuthorId() != null
                && !event.getCommentAuthorId().equals(event.getParentCommentAuthorId())) {

            NotificationCommand replyCommand = new NotificationCommand(
                event.getParentCommentAuthorId(),
                event.getCommentAuthorId(),
                NotificationType.REPLY_ON_MY_COMMENT,
                "새 답글",
                NotificationType.REPLY_ON_MY_COMMENT
                    .formatMessage(event.getCommentAuthorName()),
                "/boards/" + event.getBoardId(),
                event.getCommentId()
            );
            sendNotificationUseCase.send(replyCommand);
        }
    }

    /**
     * 팔로우 시 → 팔로우 당한 사람에게 알림
     */
    @Async("notificationExecutor")
    @EventListener
    public void handleFollowCreated(FollowCreatedEvent event) {
        log.info("FollowCreatedEvent 수신: {} → {}",
            event.getFollowerId(), event.getFollowingId());

        NotificationCommand command = NotificationCommand.forNewFollower(
            event.getFollowingId(),
            event.getFollowerId(),
            event.getFollowerName()
        );
        sendNotificationUseCase.send(command);
    }
}
```

---

## 7. 비동기 처리 설정

### 7.1 Async 설정

```java
// common/config/AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // 기본 스레드 수
        executor.setMaxPoolSize(5);       // 최대 스레드 수
        executor.setQueueCapacity(100);   // 큐 크기
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // 기본 Async 예외 처리
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }
}

// common/config/CustomAsyncExceptionHandler.java
@Slf4j
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("비동기 작업 예외 발생 - 메서드: {}, 파라미터: {}",
            method.getName(), Arrays.toString(params), ex);

        // 필요시 Slack 알림, 재시도 큐 등 추가 처리
    }
}
```

### 7.2 application.yml 설정

```yaml
# application.yml
spring:
  task:
    execution:
      pool:
        core-size: 2
        max-size: 5
        queue-capacity: 100
      thread-name-prefix: async-

# FCM 설정 (Firebase)
firebase:
  credentials-path: classpath:firebase-service-account.json
```

---

## 8. 알림 타입별 구현

### 8.1 전체 알림 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                        알림 흐름 전체 구조                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [사용자 액션]                                                       │
│       │                                                             │
│       ▼                                                             │
│  ┌─────────────┐     이벤트 발행      ┌──────────────────┐          │
│  │   Service   │ ─────────────────→  │  Event Listener  │          │
│  │ (동기 처리)  │                      │  (@Async 비동기)  │          │
│  └─────────────┘                      └────────┬─────────┘          │
│       │                                        │                    │
│       │ 즉시 응답                               │ 백그라운드          │
│       ▼                                        ▼                    │
│  ┌─────────────┐                      ┌──────────────────┐          │
│  │   Response  │                      │ NotificationSvc  │          │
│  │  (0.1초)    │                      │  - DB 저장        │          │
│  └─────────────┘                      │  - FCM 발송       │          │
│                                       └──────────────────┘          │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 알림 타입별 처리 요약

| 이벤트 | 알림 받는 사람 | 알림 타입 |
|-------|--------------|----------|
| 게시글 작성 | 작성자의 팔로워들 | FOLLOWING_NEW_BOARD |
| 댓글 작성 | 게시글 작성자 | COMMENT_ON_MY_BOARD |
| 대댓글 작성 | 부모 댓글 작성자 | REPLY_ON_MY_COMMENT |
| 팔로우 | 팔로우 대상 | NEW_FOLLOWER |
| 좋아요 | 게시글 작성자 | LIKE_ON_MY_BOARD |

---

## 9. 테스트 전략

### 9.1 Domain 테스트

```java
class NotificationTest {

    @Test
    void 알림_생성_성공() {
        Notification notification = Notification.create(
            1L, 2L, NotificationType.COMMENT_ON_MY_BOARD,
            "새 댓글", "홍길동님이 댓글을 남겼습니다.",
            "/boards/1", 1L
        );

        assertThat(notification.getReceiverId()).isEqualTo(1L);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void 알림_읽음_처리() {
        Notification notification = Notification.create(
            1L, 2L, NotificationType.NEW_FOLLOWER,
            "새 팔로워", "메시지", "/users/2", 2L
        );

        notification.markAsRead();

        assertThat(notification.isRead()).isTrue();
    }
}
```

### 9.2 Service 테스트 (Mock)

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void 알림_발송_성공() {
        // given
        NotificationCommand command = NotificationCommand.forNewFollower(
            1L, 2L, "홍길동"
        );

        User receiver = User.create("receiver@test.com", "password", "수신자");
        ReflectionTestUtils.setField(receiver, "id", 1L);
        ReflectionTestUtils.setField(receiver, "fcmToken", "test-token");

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(receiver));

        // when
        notificationService.send(command);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationSender).send(any(PushNotification.class));
    }
}
```

### 9.3 이벤트 리스너 테스트

```java
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private SendNotificationUseCase sendNotificationUseCase;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private NotificationEventListener eventListener;

    @Test
    void 게시글_작성시_팔로워에게_알림_발송() {
        // given
        BoardCreatedEvent event = new BoardCreatedEvent(
            1L, 100L, "작성자", "게시글 제목"
        );

        when(followRepository.findFollowerIdsByFollowingId(100L))
            .thenReturn(List.of(1L, 2L, 3L));

        // when
        eventListener.handleBoardCreated(event);

        // then
        verify(sendNotificationUseCase).sendToMultiple(argThat(commands ->
            commands.size() == 3 &&
            commands.stream().allMatch(c ->
                c.type() == NotificationType.FOLLOWING_NEW_BOARD)
        ));
    }

    @Test
    void 본인_글에_본인_댓글시_알림_발송_안함() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
            1L, 10L, 100L, 100L, "작성자", "게시글", null, null
        );

        // when
        eventListener.handleCommentCreated(event);

        // then
        verify(sendNotificationUseCase, never()).send(any());
    }
}
```

### 9.4 통합 테스트

```java
@SpringBootTest
@Transactional
class NotificationIntegrationTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Test
    void 게시글_작성시_팔로워에게_알림_저장() throws InterruptedException {
        // given
        User author = userRepository.save(
            User.create("author@test.com", "password", "작성자"));
        User follower = userRepository.save(
            User.create("follower@test.com", "password", "팔로워"));

        followRepository.save(Follow.create(follower.getId(), author.getId()));

        // when
        boardService.create(
            new CreateBoardCommand("제목", "내용"),
            author.getId()
        );

        // 비동기 처리 대기
        Thread.sleep(1000);

        // then
        List<Notification> notifications = notificationRepository
            .findByReceiverId(follower.getId());

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType())
            .isEqualTo(NotificationType.FOLLOWING_NEW_BOARD);
    }
}
```

---

## 10. 단계별 확장 가이드

### Stage 1: 현재 (단일 서버 + @Async)

```
┌─────────────────────────────────────┐
│            API 서버 (1대)            │
│  ┌─────────────┐  ┌──────────────┐  │
│  │  API 요청   │  │ @Async 스레드 │  │
│  │   처리      │→ │  알림 발송    │  │
│  └─────────────┘  └──────────────┘  │
└─────────────────────────────────────┘

적합: 일일 알림 10,000건 이하
```

### Stage 2: Redis Queue 추가

```
┌─────────────────────────────────────────────────┐
│                 API 서버 (1대)                   │
│  ┌─────────────┐                                │
│  │  API 요청   │                                │
│  │   처리      │                                │
│  └──────┬──────┘                                │
│         │ 이벤트 발행                            │
│         ▼                                       │
│  ┌─────────────┐     ┌────────────────────┐    │
│  │ Redis Queue │ ←── │ @Scheduled Consumer │    │
│  │  (대기열)    │ ──→ │    알림 발송         │    │
│  └─────────────┘     └────────────────────┘    │
└─────────────────────────────────────────────────┘

적합: 일일 알림 100,000건 이하, 재시도 필요
```

```java
// Redis Queue 사용 예시
@Service
public class NotificationQueueService {

    private final StringRedisTemplate redisTemplate;

    public void enqueue(NotificationCommand command) {
        redisTemplate.opsForList().rightPush(
            "notification:queue",
            objectMapper.writeValueAsString(command)
        );
    }
}

@Component
public class NotificationQueueConsumer {

    @Scheduled(fixedDelay = 1000)
    public void processQueue() {
        String json = redisTemplate.opsForList()
            .leftPop("notification:queue");
        if (json != null) {
            NotificationCommand command = objectMapper.readValue(json, ...);
            notificationService.send(command);
        }
    }
}
```

### Stage 3: 메시지 브로커 (Kafka/SQS)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│  API 서버    │ ──→ │  Kafka/SQS   │ ──→ │ Notification     │
│  (Producer)  │     │  (Queue)     │     │ Worker (별도)    │
└──────────────┘     └──────────────┘     └──────────────────┘

적합: 일일 알림 1,000,000건 이상, 독립 확장 필요
```

### Stage 4: 마이크로서비스

```
community-api/           ← 메인 API (별도 레포)
notification-service/    ← 알림 서비스 (별도 레포, 별도 배포)
```

### 확장 시점 판단

| 지표 | Stage 1 유지 | Stage 2 | Stage 3+ |
|-----|-------------|---------|----------|
| 일일 알림 | < 10K | 10K-100K | > 100K |
| 알림 지연 | 1-2초 OK | 실시간 필요 | 실시간 + 안정성 |
| 재시도 | 불필요 | 필요 | 필수 |
| 팀 규모 | 1-3명 | 3-5명 | 5명+ |

---

## 요약

### 핵심 구조

```
notification/
├── domain/           # 순수 도메인 (Notification, NotificationType)
├── application/
│   ├── port/in/      # Use Case 인터페이스
│   ├── port/out/     # NotificationSender 인터페이스
│   └── service/      # 비즈니스 로직 + @Async
└── infrastructure/
    ├── persistence/  # JPA 구현
    ├── web/          # REST Controller
    └── external/     # FCM/Email 구현체
```

### 알림 발송 흐름

```
1. Service에서 이벤트 발행 (ApplicationEventPublisher)
2. @Async EventListener가 비동기로 수신
3. NotificationService.send() 호출
4. DB 저장 + FCM 발송
```

### 서버 분리 없이 비동기 처리

```java
@Async("notificationExecutor")
@EventListener
public void handleBoardCreated(BoardCreatedEvent event) {
    // 백그라운드에서 실행 (API 응답에 영향 없음)
}
```

---

## 다음 단계

- [Clean Architecture 기초](CLEAN_ARCHITECTURE_TUTORIAL.md)
- [테스트 모듈화](SPRING_TEST_MODULARIZATION.md)
- [전체 문서 가이드](INDEX.md)
