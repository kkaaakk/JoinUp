# JoinUp Phase 4: joinup-user Module

## Goal
Implement user registration, login, JWT authentication, current profile query, profile update, and credit score query.

## Module Placement
- `joinup-user`
- `joinup-infra`
- `joinup-common`

## Authentication Flow
1. Client calls `POST /api/user/login` with `identifier` and `password`.
2. `UserController` delegates to `UserService.login(...)`.
3. `UserServiceImpl` loads the user by username, phone, or email.
4. `PasswordEncoder` verifies the raw password against the stored hash.
5. User status is checked before login is accepted.
6. `last_login_at` is updated on successful login.
7. `JwtTokenProvider` creates a JWT from `LoginUser`.
8. Protected APIs use `Authorization: Bearer <token>`.
9. `JwtAuthenticationFilter` parses the token and writes `LoginUser` into the Spring Security context.
10. Protected controllers read the current user by `@AuthenticationPrincipal LoginUser`.

## API Examples

### Register
`POST /api/user/register`

```json
{
  "username": "joinup_user",
  "password": "12345678",
  "confirmPassword": "12345678",
  "phone": "13800138000",
  "email": "user@example.com",
  "nickname": "joinup user"
}
```

### Login
`POST /api/user/login`

```json
{
  "identifier": "joinup_user",
  "password": "12345678"
}
```

### Get Current Profile
`GET /api/user/profile`

```http
Authorization: Bearer <access_token>
```

### Update Current Profile
`PUT /api/user/profile`

```json
{
  "nickname": "new_nickname",
  "phone": "13800138000",
  "email": "user@example.com",
  "avatarUrl": "https://example.com/avatar.png",
  "gender": 1,
  "birthday": "2000-01-01",
  "schoolName": "JoinUp University",
  "major": "Computer Science",
  "bio": "Love badminton and study groups.",
  "city": "Shanghai"
}
```

### Get Current Credit
`GET /api/user/credit`

## Implemented Files
- `joinup-user/src/main/java/com/joinup/user/controller/UserController.java`
- `joinup-user/src/main/java/com/joinup/user/service/UserService.java`
- `joinup-user/src/main/java/com/joinup/user/service/impl/UserServiceImpl.java`
- `joinup-user/src/main/java/com/joinup/user/mapper/UserMapper.java`
- `joinup-user/src/main/java/com/joinup/user/mapper/UserProfileMapper.java`
- `joinup-user/src/main/java/com/joinup/user/dto/*.java`
- `joinup-user/src/main/java/com/joinup/user/vo/*.java`
- `joinup-infra/src/main/java/com/joinup/infrastructure/security/JwtTokenProvider.java`
- `joinup-infra/src/main/java/com/joinup/infrastructure/security/JwtAuthenticationFilter.java`
- `joinup-infra/src/main/java/com/joinup/infrastructure/security/SecurityConfig.java`

## Notes
- Passwords are stored with `BCryptPasswordEncoder`.
- User status is enforced during login and self-service APIs.
- JWT claims already keep enough user context for later RBAC expansion.
