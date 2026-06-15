# PETCARE BACKEND CODING CONVENTION

> Dự án: PetCare Backend  
> Công nghệ: Java Spring Boot, Spring Security, JWT, JPA/Hibernate, MySQL 8  
> Package gốc hiện tại: `com.petcare.backend`  
> Mục tiêu tài liệu: dùng làm chuẩn khi code backend và làm ngữ cảnh cho IDE AI/Copilot/Cursor/Codex.

---

## 1. Nguyên tắc tổng quát

### 1.1. Backend chỉ trả dữ liệu, không xử lý UI

Backend chịu trách nhiệm:

- Xác thực và phân quyền.
- Kiểm tra dữ liệu đầu vào.
- Thực thi nghiệp vụ.
- Truy vấn và cập nhật database.
- Trả response JSON thống nhất.
- Ghi nhận lỗi, exception, audit/timeline nếu cần.

Backend không chịu trách nhiệm:

- Format giao diện.
- Quyết định layout mobile/web.
- Render HTML.
- Lưu trực tiếp file nhị phân lớn trong database.

---

### 1.2. Không trả thẳng Entity ra API

Controller không được trả trực tiếp entity JPA như `User`, `Post`, `Pet`.

Sai:

```java
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```

Đúng:

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserProfileResponse>> getUser(@PathVariable Long id) {
    UserProfileResponse response = userService.getUserProfile(id);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

Lý do:

- Tránh lộ dữ liệu nhạy cảm như `passwordHash`, token, internal status.
- Tránh lỗi vòng lặp JSON do quan hệ JPA hai chiều.
- Dễ kiểm soát dữ liệu trả về cho FE/mobile.
- Dễ thay đổi entity mà không phá API contract.

---

### 1.3. Controller mỏng, Service dày

Controller chỉ làm các việc sau:

1. Nhận request.
2. Validate DTO bằng `@Valid`.
3. Lấy user hiện tại từ Spring Security nếu API cần đăng nhập.
4. Gọi service.
5. Bọc response bằng `ApiResponse`.

Controller không được:

- Gọi repository trực tiếp.
- Viết logic nghiệp vụ.
- Tự kiểm tra quyền phức tạp.
- Tự convert entity quá nhiều.
- Tự bắt try/catch cho mọi lỗi nghiệp vụ.

Logic nghiệp vụ phải nằm trong service hoặc service hỗ trợ.

---

### 1.4. Repository chỉ truy vấn database

Repository chỉ dùng cho:

- Query method của Spring Data JPA.
- `@Query` JPQL/native SQL khi cần.
- Kiểm tra tồn tại dữ liệu.
- Phân trang.

Repository không được:

- Kiểm tra quyền nghiệp vụ.
- Tạo response DTO phức tạp nếu không có lý do rõ ràng.
- Gửi email.
- Tạo JWT.
- Upload file.
- Gọi service khác.

---

### 1.5. Service là nơi xử lý nghiệp vụ chính

Service chịu trách nhiệm:

- Kiểm tra user đã đăng nhập hay chưa.
- Kiểm tra tài khoản có `active` không.
- Kiểm tra dữ liệu có tồn tại không.
- Kiểm tra quyền owner/co-editor/co-viewer/follower.
- Kiểm tra trạng thái dữ liệu như `published`, `hidden`, `deleted`, `archived`, `deceased`.
- Gọi repository.
- Mapping entity sang response DTO.
- Gửi email/notification thông qua service chuyên trách.
- Ghi timeline/audit log nếu có.

---

## 2. Cấu trúc thư mục chuẩn theo project hiện tại

Cấu trúc hiện tại đang đi theo kiểu **layer-based architecture**. Tiếp tục giữ kiểu này để không phải refactor lớn.

```text
src/main/java/com/petcare/backend
├── config
├── controller
├── dto
│   ├── auth
│   │   ├── request
│   │   └── response
│   ├── common
│   ├── user
│   │   ├── request
│   │   └── response
│   └── social
│       ├── request
│       └── response
├── exception
├── model
├── repository
├── security
├── service
│   └── impl
└── BackendApplication.java
```

---

## 3. Quy ước từng thư mục

### 3.1. `config`

Chứa các class cấu hình Spring Boot.

Ví dụ hiện có:

```text
config
├── SecurityConfig.java
└── SwaggerConfig.java
```

Quy ước:

- Tên class luôn kết thúc bằng `Config`.
- Không chứa logic nghiệp vụ.
- Không gọi repository trong config, trừ trường hợp đặc biệt và có giải thích.
- Không hard-code secret, password, token.
- Các giá trị cấu hình lấy từ `application.properties`, `.env` hoặc biến môi trường.

Ví dụ tên class hợp lệ:

```text
SecurityConfig.java
SwaggerConfig.java
CorsConfig.java
MailConfig.java
FileStorageConfig.java
```

---

### 3.2. `controller`

Chứa REST API controller.

Quy ước đặt tên:

```text
AuthController.java
UserController.java
PostController.java
CommentController.java
ReactionController.java
FollowController.java
NewsfeedController.java
PetController.java
```

Quy tắc:

- Mỗi controller phụ trách một nhóm API rõ ràng.
- Class phải có `@RestController`.
- Class phải có `@RequestMapping` ở cấp class.
- Method phải trả `ResponseEntity<ApiResponse<T>>` hoặc `ResponseEntity<ApiResponse<Void>>`.
- Không trả entity trực tiếp.
- Không gọi repository trực tiếp.
- Luôn dùng DTO request/response.
- Dùng `@Valid` cho request body.
- Dùng `@AuthenticationPrincipal UserPrincipal currentUser` cho API cần đăng nhập.

Ví dụ:

```java
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreatePostRequest request
    ) {
        PostResponse response = postService.createPost(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }
}
```

---

### 3.3. `dto`

Chứa các object dùng để nhận request và trả response.

Không để entity JPA trong `dto`.

Cấu trúc chuẩn:

```text
dto
├── common
│   ├── ApiResponse.java
│   ├── PageResponse.java
│   └── ErrorResponse.java
├── auth
│   ├── request
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── ForgotPasswordRequest.java
│   │   └── ResetPasswordRequest.java
│   └── response
│       ├── AuthResponse.java
│       └── TokenResponse.java
├── user
│   ├── request
│   │   ├── UpdateProfileRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   └── UpdateUserPreferencesRequest.java
│   └── response
│       ├── UserProfileResponse.java
│       └── UserPreferencesResponse.java
└── social
    ├── request
    │   ├── CreatePostRequest.java
    │   ├── UpdatePostRequest.java
    │   ├── CreateCommentRequest.java
    │   └── ReactRequest.java
    └── response
        ├── PostResponse.java
        ├── CommentResponse.java
        ├── ReactionSummaryResponse.java
        └── NewsfeedItemResponse.java
```

---

#### 3.3.1. Request DTO

Tên class request phải kết thúc bằng `Request`.

Ví dụ:

```text
CreatePostRequest
UpdateProfileRequest
ChangePasswordRequest
CreateCommentRequest
ReactRequest
```

Quy tắc:

- Chỉ chứa field client gửi lên.
- Không chứa field hệ thống tự sinh như `id`, `createdAt`, `updatedAt`, `status` nếu client không được phép chỉnh.
- Phải có validation annotation nếu dữ liệu bắt buộc.
- Không chứa logic nghiệp vụ.

Ví dụ:

```java
@Getter
@Setter
public class CreatePostRequest {

    @Size(max = 2000, message = "Caption không được vượt quá 2000 ký tự")
    private String caption;

    private Long petId;

    @NotNull(message = "Quyền riêng tư không được để trống")
    private PostPrivacy privacy;

    private List<AttachmentRequest> attachments;
}
```

---

#### 3.3.2. Response DTO

Tên class response phải kết thúc bằng `Response`.

Ví dụ:

```text
UserProfileResponse
PostResponse
CommentResponse
FollowResponse
```

Quy tắc:

- Chỉ chứa dữ liệu được phép trả về.
- Không chứa password hash, reset token, refresh token nội bộ, OTP.
- Field dùng `camelCase` để JSON trả về thân thiện với FE/mobile.
- Có thể chứa object con nếu cần.

Ví dụ:

```java
@Getter
@Setter
@Builder
public class PostResponse {
    private Long id;
    private Long authorId;
    private String authorName;
    private Long petId;
    private String petName;
    private String caption;
    private PostPrivacy privacy;
    private PostStatus status;
    private List<PostImageResponse> images;
    private Long totalReactions;
    private Long totalComments;
    private LocalDateTime createdAt;
}
```

---

### 3.4. `dto/common`

Chứa các DTO dùng chung toàn hệ thống.

Bắt buộc giữ response format thống nhất.

Mẫu `ApiResponse` khuyến nghị:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Tạo mới thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiResponse<Void> successMessage(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
```

Mẫu `PageResponse`:

```java
@Getter
@Setter
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
```

---

### 3.5. `exception`

Chứa custom exception và global exception handler.

Hiện có:

```text
BadRequestException.java
GlobalExceptionHandler.java
ResourceNotFoundException.java
```

Nên bổ sung khi cần:

```text
UnauthorizedException.java
ForbiddenException.java
ConflictException.java
BannedUserException.java
InvalidTokenException.java
```

Quy tắc:

- Không throw `RuntimeException` chung chung trong service.
- Không trả stack trace cho client.
- Lỗi nghiệp vụ phải có exception rõ nghĩa.
- Tất cả exception được xử lý tập trung ở `GlobalExceptionHandler`.

Ví dụ custom exception:

```java
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
```

Ví dụ handler:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
```

Quy ước HTTP status:

| Trường hợp | HTTP status |
|---|---:|
| Chưa đăng nhập hoặc token sai | 401 |
| Đăng nhập rồi nhưng không có quyền | 403 |
| Không tìm thấy dữ liệu | 404 |
| Request sai dữ liệu | 400 |
| Dữ liệu trùng hoặc trạng thái xung đột | 409 |
| Tạo mới thành công | 201 |
| Cập nhật/xóa mềm thành công | 200 |

---

### 3.6. `model`

Chứa JPA entity ánh xạ với bảng MySQL.

Hiện có ví dụ:

```text
EmailVerificationToken.java
Notification.java
PasswordResetToken.java
RefreshToken.java
User.java
UserDevice.java
```

Quy tắc đặt tên:

| Database table | Java entity |
|---|---|
| `users` | `User` |
| `user_devices` | `UserDevice` |
| `password_reset_tokens` hoặc `password_resets` | `PasswordResetToken` |
| `posts` | `Post` |
| `post_comments` | `PostComment` |
| `post_images` | `PostImage` |
| `post_reactions` | `PostReaction` |

Entity phải dùng tên số ít.

---

#### 3.6.1. Quy tắc annotation entity

Mẫu entity chuẩn:

```java
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostPrivacy privacy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostStatus status;

    @Column(name = "comments_locked", nullable = false)
    private Boolean commentsLocked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

---

#### 3.6.2. Entity không được làm gì?

Entity không được:

- Gọi repository.
- Gọi service.
- Gửi email.
- Sinh JWT.
- Trả response DTO.
- Chứa logic phân quyền phức tạp.

Entity chỉ nên chứa:

- Field mapping database.
- Quan hệ JPA.
- Enum.
- Method nhỏ để cập nhật trạng thái nếu thật sự cần.

---

#### 3.6.3. Quan hệ JPA

Mặc định dùng `FetchType.LAZY` cho `@ManyToOne`, `@OneToMany`, `@OneToOne`.

Sai:

```java
@ManyToOne(fetch = FetchType.EAGER)
private User user;
```

Đúng:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

Không nên lạm dụng quan hệ hai chiều.

Nếu chưa cần load ngược từ `User` sang `Post`, không thêm:

```java
@OneToMany(mappedBy = "user")
private List<Post> posts;
```

Lý do:

- Tránh vòng lặp JSON.
- Tránh query nặng.
- Tránh khó debug.

---

#### 3.6.4. Kiểu dữ liệu chuẩn

| Loại dữ liệu | Java | MySQL |
|---|---|---|
| ID | `Long` | `BIGINT UNSIGNED` |
| Text ngắn | `String` | `VARCHAR` |
| Text dài | `String` | `TEXT` |
| Boolean | `Boolean` | `BOOLEAN` |
| Ngày | `LocalDate` | `DATE` |
| Giờ | `LocalTime` | `TIME` |
| Ngày giờ | `LocalDateTime` | `TIMESTAMP` hoặc `DATETIME` |
| Tiền | `BigDecimal` | `DECIMAL(10,2)` |
| Enum | Java enum | `VARCHAR` hoặc MySQL `ENUM` |

Không dùng `double` hoặc `float` cho tiền.

---

#### 3.6.5. Boolean field

Ưu tiên đặt tên field boolean không bắt đầu bằng `is` trong Java entity.

Nên:

```java
private Boolean active;
private Boolean commentsLocked;
private Boolean pushNotificationsEnabled;
```

Không nên:

```java
private Boolean isActive;
private Boolean isCommentsLocked;
```

Lý do: tránh lỗi getter/setter khi dùng Lombok/Jackson/JPA.

---

### 3.7. `repository`

Chứa Spring Data JPA Repository.

Hiện có:

```text
EmailVerificationTokenRepository.java
NotificationRepository.java
PasswordResetTokenRepository.java
RefreshTokenRepository.java
UserDeviceRepository.java
UserRepository.java
```

Quy tắc đặt tên:

```text
<EntityName>Repository.java
```

Ví dụ:

```text
PostRepository.java
PostCommentRepository.java
PostImageRepository.java
PostReactionRepository.java
FollowRepository.java
PetRepository.java
```

Mẫu repository:

```java
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            PostStatus status,
            Pageable pageable
    );

    Page<Post> findByPetIdAndStatusOrderByCreatedAtDesc(
            Long petId,
            PostStatus status,
            Pageable pageable
    );
}
```

Quy tắc:

- Dùng `Optional<T>` cho query có thể không tìm thấy một bản ghi.
- Dùng `existsBy...` để kiểm tra tồn tại.
- Dùng `Page<T>` cho danh sách có phân trang.
- Không trả `List<T>` cho API danh sách lớn.
- Không viết native SQL nếu JPQL/query method đủ dùng.

Ví dụ:

```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
```

---

### 3.8. `security`

Chứa các class liên quan xác thực, JWT, Spring Security.

Hiện có:

```text
CustomUserDetailsService.java
JwtAuthenticationFilter.java
JwtService.java
UserPrincipal.java
```

Quy tắc:

- `JwtService` chỉ xử lý tạo/parse/validate token.
- `JwtAuthenticationFilter` chỉ đọc token từ request và set Authentication vào SecurityContext.
- `CustomUserDetailsService` chỉ load user cho Spring Security.
- `UserPrincipal` chứa thông tin user cần thiết cho security context.
- Không viết logic nghiệp vụ social/pet/user profile trong package `security`.

Khi service cần user hiện tại, controller truyền `currentUser.getId()` vào service.

Nên:

```java
@PostMapping
public ResponseEntity<ApiResponse<PostResponse>> createPost(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @Valid @RequestBody CreatePostRequest request
) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(postService.createPost(currentUser.getId(), request)));
}
```

Không nên để service tự đọc `SecurityContextHolder` trừ khi có lý do rõ ràng.

---

### 3.9. `service`

Chứa interface service.

Hiện có:

```text
AuthService.java
EmailService.java
EmailVerificationService.java
UserService.java
```

Quy tắc:

- Interface đặt trong `service`.
- Implementation đặt trong `service/impl`.
- Interface chỉ khai báo method public.
- Không để logic trong interface.

Ví dụ:

```java
public interface PostService {
    PostResponse createPost(Long currentUserId, CreatePostRequest request);
    PostResponse getPostById(Long currentUserId, Long postId);
    PageResponse<PostResponse> getMyPosts(Long currentUserId, Pageable pageable);
    void deletePost(Long currentUserId, Long postId);
}
```

---

### 3.10. `service/impl`

Chứa class implementation.

Hiện có:

```text
AuthServiceImpl.java
EmailServiceImpl.java
EmailVerificationServiceImpl.java
UserServiceImpl.java
```

Quy tắc đặt tên:

```text
<ServiceInterfaceName>Impl.java
```

Ví dụ:

```text
PostServiceImpl.java
CommentServiceImpl.java
ReactionServiceImpl.java
FollowServiceImpl.java
NewsfeedServiceImpl.java
```

Mẫu implementation:

```java
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final SocialPermissionService socialPermissionService;

    @Override
    @Transactional
    public PostResponse createPost(Long currentUserId, CreatePostRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        socialPermissionService.checkUserActive(user);

        // business logic here
        return null;
    }
}
```

---

## 4. Quy ước package khi thêm chức năng mới

Vì dự án hiện đang dùng layer-based architecture, khi thêm chức năng mới không tạo package lung tung.

Ví dụ thêm chức năng social:

```text
controller
├── PostController.java
├── CommentController.java
├── ReactionController.java
├── FollowController.java
└── NewsfeedController.java

dto
└── social
    ├── request
    │   ├── CreatePostRequest.java
    │   ├── UpdatePostRequest.java
    │   ├── CreateCommentRequest.java
    │   ├── UpdateCommentRequest.java
    │   └── ReactRequest.java
    └── response
        ├── PostResponse.java
        ├── PostImageResponse.java
        ├── CommentResponse.java
        ├── ReactionSummaryResponse.java
        ├── FollowResponse.java
        └── NewsfeedItemResponse.java

model
├── Post.java
├── PostImage.java
├── PostComment.java
├── PostReaction.java
├── CommentImage.java
├── CommentReaction.java
└── Follow.java

repository
├── PostRepository.java
├── PostImageRepository.java
├── PostCommentRepository.java
├── PostReactionRepository.java
├── CommentImageRepository.java
├── CommentReactionRepository.java
└── FollowRepository.java

service
├── PostService.java
├── CommentService.java
├── ReactionService.java
├── FollowService.java
├── NewsfeedService.java
└── SocialPermissionService.java

service/impl
├── PostServiceImpl.java
├── CommentServiceImpl.java
├── ReactionServiceImpl.java
├── FollowServiceImpl.java
├── NewsfeedServiceImpl.java
└── SocialPermissionServiceImpl.java
```

Không nên tạo kiểu lẫn lộn:

```text
post/PostController.java
post/PostService.java
controller/social/PostController.java
service/post/PostService.java
```

Trừ khi quyết định refactor toàn dự án sang feature-based architecture.

---

## 5. Quy ước đặt tên code Java

### 5.1. Package name

Tất cả package viết thường.

Đúng:

```text
com.petcare.backend.controller
com.petcare.backend.dto.user.request
com.petcare.backend.service.impl
```

Sai:

```text
com.petcare.backend.Controller
com.petcare.backend.DTO.User.Request
```

---

### 5.2. Class name

Class dùng `PascalCase`.

```text
UserController
PostServiceImpl
CreatePostRequest
PostResponse
ResourceNotFoundException
```

---

### 5.3. Method name

Method dùng `camelCase` và bắt đầu bằng động từ.

Đúng:

```java
createPost()
updateProfile()
deleteComment()
getCurrentUserProfile()
findPublishedPostById()
checkCanViewPost()
```

Sai:

```java
postCreate()
profileUpdate()
comment_delete()
GetUser()
```

---

### 5.4. Variable name

Biến dùng `camelCase`, tên rõ nghĩa.

Đúng:

```java
currentUserId
postId
profileUserId
createdPost
publishedPosts
```

Sai:

```java
x
u
p
list1
abc
```

---

### 5.5. Constant name

Constant dùng `UPPER_SNAKE_CASE`.

```java
private static final int MAX_POST_IMAGES = 10;
private static final int MAX_COMMENT_DEPTH = 1;
private static final String DEFAULT_LANGUAGE = "vi";
```

---

## 6. Quy ước database MySQL

### 6.1. Tên bảng

Tên bảng dùng `snake_case`, số nhiều nếu là bảng dữ liệu chính.

Đúng:

```text
users
pets
posts
post_comments
post_images
post_reactions
user_devices
password_reset_tokens
```

Không nên:

```text
User
PostComment
postComment
TblUsers
```

---

### 6.2. Tên cột

Tên cột dùng `snake_case`.

Đúng:

```text
id
user_id
pet_id
created_at
updated_at
avatar_url
password_hash
comments_locked
```

Sai:

```text
userId
createdAt
AvatarUrl
```

---

### 6.3. Khóa chính

Mỗi bảng chính nên có:

```sql
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY
```

Bảng quan hệ nhiều-nhiều có thể dùng khóa chính kép.

Ví dụ:

```sql
PRIMARY KEY (post_id, user_id)
```

---

### 6.4. Khóa ngoại

Tên khóa ngoại theo mẫu:

```text
fk_<table>_<referenced_table>
```

Ví dụ:

```sql
CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id)
CONSTRAINT fk_posts_pet FOREIGN KEY (pet_id) REFERENCES pets(id)
```

---

### 6.5. Index

Các cột thường dùng để lọc hoặc join phải có index.

Ví dụ:

```sql
INDEX idx_posts_user_status_created (user_id, status, created_at)
INDEX idx_posts_pet_status_created (pet_id, status, created_at)
INDEX idx_comments_post_created (post_id, created_at)
INDEX idx_follows_follower_status (follower_id, status)
```

---

### 6.6. Enum trong MySQL và Java

Với Java Spring Boot + JPA, nên ưu tiên Java enum với `@Enumerated(EnumType.STRING)`.

Ví dụ:

```java
public enum PostStatus {
    PUBLISHED,
    HIDDEN,
    DELETED
}
```

Trong entity:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private PostStatus status;
```

Nếu database đang dùng MySQL `ENUM('published','hidden','deleted')`, cần đồng bộ cẩn thận với Java enum. Có hai cách:

Cách 1: Java enum dùng chữ hoa, DB lưu chữ hoa.

```text
PUBLISHED, HIDDEN, DELETED
```

Cách 2: Java enum map sang chữ thường bằng converter.

Với đồ án, cách dễ nhất là dùng Java enum chữ hoa và DB `VARCHAR(30)`. Nếu schema hiện tại đã dùng MySQL ENUM chữ thường, service phải mapping thống nhất, không viết lẫn lộn.

---

## 7. Quy ước validation

Tất cả request DTO phải validate ở tầng controller bằng `@Valid`.

Ví dụ:

```java
@PostMapping
public ResponseEntity<ApiResponse<PostResponse>> createPost(
        @Valid @RequestBody CreatePostRequest request
) {
    // ...
}
```

Các annotation thường dùng:

```java
@NotBlank
@NotNull
@Email
@Size
@Min
@Max
@Pattern
```

Ví dụ:

```java
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    private String fullName;
}
```

---

## 8. Quy ước transaction

### 8.1. Method ghi dữ liệu

Các method tạo/sửa/xóa phải có `@Transactional`.

```java
@Transactional
public PostResponse createPost(Long currentUserId, CreatePostRequest request) {
    // insert/update/delete
}
```

---

### 8.2. Method chỉ đọc dữ liệu

Các method chỉ đọc dùng:

```java
@Transactional(readOnly = true)
```

Ví dụ:

```java
@Transactional(readOnly = true)
public PostResponse getPostById(Long currentUserId, Long postId) {
    // select only
}
```

---

### 8.3. Không mở transaction ở controller

Sai:

```java
@Transactional
@PostMapping
public ResponseEntity<?> createPost(...) {
    // ...
}
```

Đúng: transaction đặt ở service.

---

## 9. Quy ước API endpoint

### 9.1. Base path

Nếu project hiện tại đã dùng `/api`, tiếp tục dùng `/api` cho nhất quán.

Ví dụ:

```text
/api/auth/register
/api/auth/login
/api/users/me
/api/posts
/api/newsfeed
```

Nếu muốn versioning sau này, dùng:

```text
/api/v1/posts
```

Không trộn lẫn `/api` và `/api/v1` trong cùng một giai đoạn nếu chưa thống nhất.

---

### 9.2. HTTP method

| Hành động | HTTP method |
|---|---|
| Tạo mới | `POST` |
| Lấy dữ liệu | `GET` |
| Cập nhật toàn phần hoặc nghiệp vụ update | `PUT` |
| Cập nhật một phần | `PATCH` |
| Xóa mềm hoặc xóa quan hệ | `DELETE` |

Ví dụ social:

```http
POST   /api/posts
GET    /api/posts/{postId}
PUT    /api/posts/{postId}
DELETE /api/posts/{postId}

POST   /api/posts/{postId}/comments
GET    /api/posts/{postId}/comments
DELETE /api/comments/{commentId}

PUT    /api/posts/{postId}/reaction
DELETE /api/posts/{postId}/reaction

POST   /api/users/{userId}/follow
DELETE /api/users/{userId}/follow
GET    /api/newsfeed
```

---

### 9.3. Phân trang

API danh sách phải hỗ trợ phân trang.

Request:

```http
GET /api/posts?page=0&size=10&sort=createdAt,desc
```

Quy tắc:

- `page` bắt đầu từ 0.
- `size` mặc định 10 hoặc 20.
- Giới hạn `size` tối đa, ví dụ 50.
- Response dùng `PageResponse<T>`.

---

## 10. Quy ước security và phân quyền

### 10.1. API public và API private

API public:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

API private cần JWT:

```text
GET  /api/users/me
PUT  /api/users/me
POST /api/posts
POST /api/posts/{postId}/comments
PUT  /api/posts/{postId}/reaction
GET  /api/newsfeed
```

---

### 10.2. Không tin dữ liệu userId từ request body

Sai:

```json
{
  "userId": 5,
  "caption": "Hello"
}
```

Đúng:

- Lấy user hiện tại từ JWT.
- `userId` trong request body không được quyết định người đăng.

```java
Long currentUserId = currentUser.getId();
```

---

### 10.3. Kiểm tra user active

Mọi API thao tác cần kiểm tra user có `status = active`.

Ví dụ:

```java
public void checkUserActive(User user) {
    if (user.getStatus() != UserStatus.ACTIVE) {
        throw new ForbiddenException("Tài khoản của bạn đã bị khóa");
    }
}
```

---

### 10.4. Service phân quyền riêng

Các nghiệp vụ phức tạp nên có service phân quyền riêng.

Ví dụ:

```text
SocialPermissionService
PetPermissionService
AdminPermissionService
```

`SocialPermissionService` nên chứa:

```java
void checkCanCreatePost(Long userId, Long petId);
void checkCanViewPost(Long viewerId, Post post);
void checkCanComment(Long userId, Post post);
void checkCanDeleteComment(Long userId, PostComment comment);
void checkCanReactToPost(Long userId, Post post);
```

---

## 11. Quy ước cho module Social Layer

### 11.1. Bảng và entity đề xuất

Nếu làm Social Layer mở rộng hơn MVP, nên dùng:

```text
posts
post_images hoặc post_attachments
post_reactions
post_comments
comment_images hoặc comment_attachments
comment_reactions
follows
pet_follows
```

Nếu vẫn giữ MVP tối giản, có thể dùng:

```text
posts.images
post_likes
post_comments
follows
```

Tuy nhiên, không nên dùng đồng thời cả `posts.images` và `post_images` để lưu cùng một loại dữ liệu, vì dễ lệch dữ liệu.

---

### 11.2. `posts.pet_id`

`posts.pet_id` dùng để gắn bài viết với một thú cưng cụ thể.

Mục đích:

- Gom ảnh bài viết vào album của pet.
- Đưa bài viết vào timeline của pet.
- Lọc bài viết theo hồ sơ pet.
- Hỗ trợ newsfeed nếu user follow pet.

`pet_id` nên cho phép `NULL` vì user có thể đăng bài cá nhân không gắn pet.

---

### 11.3. Rule tạo bài viết

Khi tạo bài viết:

1. User phải đăng nhập.
2. User phải active.
3. Caption và ảnh/file không được cùng rỗng.
4. Nếu có `petId`, pet phải tồn tại.
5. Nếu có `petId`, pet phải active.
6. Nếu có `petId`, user phải là owner hoặc co-editor được phép.
7. Privacy chỉ nhận `PUBLIC`, `FOLLOWERS`, `PRIVATE`.
8. Status mặc định là `PUBLISHED`.
9. Nếu bài viết gắn pet, có thể ghi timeline event `SOCIAL_POST`.

---

### 11.4. Rule xem bài viết

Người xem được xem bài nếu:

- Bài có `status = PUBLISHED`.
- Bài `PUBLIC`: user active có thể xem.
- Bài `FOLLOWERS`: chủ bài hoặc follower đã accepted có thể xem.
- Bài `PRIVATE`: chỉ chủ bài xem.
- Bài `HIDDEN`: user thường không xem được.
- Bài `DELETED`: không hiển thị.

---

### 11.5. Rule comment

Khi tạo comment:

1. User phải đăng nhập.
2. User phải active.
3. Post phải tồn tại.
4. Post phải `PUBLISHED`.
5. User phải có quyền xem post.
6. `commentsLocked = false`.
7. Comment không được rỗng cả text và attachment.
8. Nếu là reply, `parentCommentId` phải thuộc cùng `postId`.
9. Không cho reply vượt quá độ sâu đã quy định.

---

### 11.6. Rule xóa comment

Được xóa comment nếu:

- Người xóa là người viết comment.
- Hoặc người xóa là chủ bài viết.
- Hoặc người xóa là admin/moderator theo nghiệp vụ kiểm duyệt.

Xóa comment nên dùng soft delete:

```text
status = DELETED
```

Không nên xóa cứng ngay, vì còn cần kiểm duyệt/report/audit.

---

### 11.7. Rule reaction

Với post reaction:

- Một user chỉ có một reaction trên một post.
- Nếu user đổi emoji, update `reactionType`.
- Nếu user bấm bỏ cảm xúc, delete bản ghi reaction hoặc set inactive tùy thiết kế.

Với comment reaction:

- Một user chỉ có một reaction trên một comment.
- Không cho reaction vào comment đã deleted/hidden.

---

### 11.8. Rule follow

Khi follow user:

1. User phải đăng nhập.
2. User phải active.
3. Không được tự follow chính mình.
4. Không tạo trùng follow.
5. Nếu target user public: follow status `ACCEPTED`.
6. Nếu target user private: follow status `PENDING`.
7. Newsfeed chỉ lấy follow `ACCEPTED`.

---

### 11.9. Rule newsfeed

Newsfeed MVP:

- Chỉ lấy bài `PUBLISHED`.
- Lọc theo quyền xem.
- Lấy bài của user/pet đang follow.
- Sắp xếp `createdAt DESC`.
- Không dùng thuật toán đề xuất.
- Bắt buộc phân trang.

---

## 12. Quy ước mapping entity sang DTO

### 12.1. Với project nhỏ

Có thể viết private method trong service implementation:

```java
private PostResponse toPostResponse(Post post) {
    return PostResponse.builder()
            .id(post.getId())
            .caption(post.getCaption())
            .privacy(post.getPrivacy())
            .status(post.getStatus())
            .createdAt(post.getCreatedAt())
            .build();
}
```

---

### 12.2. Khi project lớn hơn

Tạo thêm package:

```text
mapper
├── UserMapper.java
├── PostMapper.java
└── CommentMapper.java
```

Quy tắc:

- Mapper chỉ convert entity ↔ DTO.
- Mapper không gọi repository.
- Mapper không kiểm tra quyền.
- Mapper không throw exception nghiệp vụ.

---

## 13. Quy ước logging

Sử dụng SLF4J.

```java
private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
```

Nếu dùng Lombok:

```java
@Slf4j
@Service
public class PostServiceImpl implements PostService {
}
```

Không log:

- Password.
- OTP.
- JWT access token.
- Refresh token.
- Secret key.
- Thông tin nhạy cảm không cần thiết.

Có thể log:

```java
log.info("User {} created post {}", currentUserId, postId);
log.warn("User {} tried to access forbidden post {}", currentUserId, postId);
log.error("Failed to send verification email to userId={}", userId, ex);
```

---

## 14. Quy ước cấu hình `application.properties`

Project đang dùng `application.properties` để cấu hình:

- Tên app.
- Port server.
- MySQL datasource.
- JPA/Hibernate.
- JWT.
- Refresh token.
- Password reset OTP.
- SMTP mail.

Quy tắc:

- Không hard-code password thật vào file.
- Secret thật phải đưa vào `.env` hoặc biến môi trường.
- `.env` không commit lên Git.
- `spring.jpa.hibernate.ddl-auto=update` chỉ dùng khi dev.
- Khi chuẩn bị production/demo ổn định, chuyển sang `validate` hoặc dùng migration tool như Flyway/Liquibase.
- `spring.jpa.show-sql=true` chỉ dùng khi dev/debug.

Ví dụ `.env`:

```properties
SERVER_PORT=9090
DB_URL=jdbc:mysql://localhost:3306/pet_care?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=change-this-secret-key
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

---

## 15. Quy ước file upload

Không lưu file nhị phân trực tiếp trong MySQL.

Database chỉ lưu metadata:

```text
file_url
thumbnail_url
original_filename
mime_type
file_size
attachment_type
display_order
created_at
```

File thật nên lưu ở:

- Local storage khi dev.
- Cloudinary.
- Amazon S3.
- Google Cloud Storage.
- Firebase Storage.
- Server file storage riêng.

Service upload nên tách riêng:

```text
FileStorageService.java
FileStorageServiceImpl.java
```

Không viết upload logic trực tiếp trong `PostServiceImpl` nếu có thể tách.

---

## 16. Quy ước test bằng Postman

Mỗi chức năng nên test theo thứ tự:

1. API thành công.
2. Thiếu token.
3. Token sai/hết hạn.
4. User bị banned.
5. Dữ liệu không tồn tại.
6. Không có quyền.
7. Dữ liệu request sai.
8. Dữ liệu trùng.
9. Trạng thái không hợp lệ.

Ví dụ test `POST /api/posts`:

```text
01_Create public post success
02_Create followers post success
03_Create private post success
04_Create post without token -> 401
05_Create post with banned user -> 403
06_Create post with invalid petId -> 404
07_Create post with pet not owned -> 403
08_Create empty post -> 400
09_Create post with invalid privacy -> 400
```

---

## 17. Checklist khi tạo một API mới

Trước khi code:

- [ ] Xác định endpoint.
- [ ] Xác định method HTTP.
- [ ] Xác định API cần login hay public.
- [ ] Xác định request DTO.
- [ ] Xác định response DTO.
- [ ] Xác định entity/bảng liên quan.
- [ ] Xác định rule phân quyền.
- [ ] Xác định exception có thể xảy ra.

Khi code:

- [ ] Tạo request DTO trong `dto/<module>/request`.
- [ ] Tạo response DTO trong `dto/<module>/response`.
- [ ] Tạo hoặc cập nhật entity trong `model`.
- [ ] Tạo repository trong `repository`.
- [ ] Tạo service interface trong `service`.
- [ ] Tạo service implementation trong `service/impl`.
- [ ] Tạo controller trong `controller`.
- [ ] Thêm exception nếu cần.
- [ ] Thêm permission service nếu logic quyền phức tạp.
- [ ] Test bằng Postman.

Sau khi code:

- [ ] Build project không lỗi.
- [ ] API trả đúng `ApiResponse`.
- [ ] Không lộ entity trực tiếp.
- [ ] Không lộ password/token/OTP.
- [ ] Có phân trang cho API danh sách.
- [ ] Có validate request.
- [ ] Có xử lý exception.
- [ ] Có test case lỗi cơ bản.

---

## 18. Quy tắc dành cho IDE AI khi sinh code

Khi dùng AI trong IDE để sinh code cho project này, luôn yêu cầu AI tuân thủ các quy tắc sau:

```text
Bạn đang code backend Java Spring Boot cho project PetCare.
Package gốc là com.petcare.backend.
Project dùng layer-based architecture gồm: config, controller, dto, exception, model, repository, security, service, service/impl.
Không tạo cấu trúc package mới nếu không được yêu cầu.
Controller chỉ nhận request, dùng @Valid, gọi service và trả ApiResponse.
Không gọi repository trực tiếp trong controller.
Không trả Entity JPA trực tiếp ra API.
Request DTO đặt trong dto/<module>/request.
Response DTO đặt trong dto/<module>/response.
Entity đặt trong model.
Repository đặt trong repository.
Service interface đặt trong service.
Service implementation đặt trong service/impl và tên kết thúc bằng Impl.
API cần đăng nhập phải lấy current user từ @AuthenticationPrincipal UserPrincipal.
Service phải kiểm tra user active, quyền truy cập và trạng thái dữ liệu.
Dùng custom exception thay vì RuntimeException chung chung.
Dùng @Transactional ở service implementation cho method ghi dữ liệu.
Dùng @Transactional(readOnly = true) cho method chỉ đọc.
Database dùng MySQL, tên bảng/cột snake_case, Java dùng camelCase.
Không hard-code secret, password, token.
```

---

## 19. Quy ước tối thiểu cho Pull Request hoặc mỗi lần hoàn thành chức năng

Trước khi coi một chức năng là xong:

- [ ] Code đúng cấu trúc thư mục.
- [ ] Không còn import thừa.
- [ ] Không còn code debug như `System.out.println`.
- [ ] Không hard-code dữ liệu test trong service.
- [ ] Không lộ password/token/OTP.
- [ ] API có request/response DTO rõ ràng.
- [ ] Có xử lý các lỗi chính.
- [ ] Có test Postman thành công.
- [ ] Có test Postman với ít nhất 3 nhánh lỗi.
- [ ] Nếu có thay đổi DB, cập nhật schema SQL hoặc migration.
- [ ] Nếu có API mới, ghi lại endpoint, method, body mẫu và response mẫu.

---

## 20. Kết luận

Với dự án hiện tại, nên giữ kiến trúc layer-based như sau:

```text
controller -> service interface -> service/impl -> repository -> model -> MySQL
```

DTO, exception, security và config phải được tách rõ.

Nguyên tắc quan trọng nhất:

```text
Controller mỏng.
Service xử lý nghiệp vụ.
Repository chỉ truy vấn.
Entity không trả trực tiếp ra API.
DTO là hợp đồng giữa backend và client.
Exception xử lý tập trung.
Security lấy user từ JWT.
Database dùng snake_case, Java dùng camelCase.
```
