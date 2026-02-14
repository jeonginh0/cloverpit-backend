# CloverPit Backend - Spring Boot

한국 배틀그라운드(PUBG) 클랜 전용 웹 애플리케이션 백엔드

## 🛠 기술 스택

- **Framework**: Spring Boot 3.2.2
- **Language**: Java 17
- **Database**: MongoDB
- **Security**: Spring Security + JWT
- **External APIs**: PUBG API, Discord JDA
- **Build Tool**: Gradle

## 📁 프로젝트 구조

```
backend/
├── src/main/java/com/cloverpit/backend/
│   ├── BackendApplication.java          # 애플리케이션 진입점
│   ├── config/
│   │   ├── SecurityConfig.java         # Spring Security 설정
│   │   └── DataInitializer.java        # 초기 데이터 설정
│   ├── controller/
│   │   ├── AuthController.java         # 인증 API
│   │   ├── PlayerController.java       # 플레이어 API
│   │   ├── MatchController.java        # 전적 API
│   │   ├── StatsController.java        # 통계 API
│   │   ├── ApplicationController.java  # 가입 신청 API
│   │   └── HealthController.java       # 헬스 체크
│   ├── dto/                            # Data Transfer Objects
│   ├── exception/
│   │   └── GlobalExceptionHandler.java # 전역 예외 처리
│   ├── model/
│   │   ├── Player.java                 # 플레이어 엔티티
│   │   ├── Match.java                  # 전적 엔티티
│   │   ├── Application.java            # 신청 엔티티
│   │   └── Admin.java                  # 관리자 엔티티
│   ├── repository/                     # MongoDB Repository
│   ├── scheduler/
│   │   └── StatsRefreshScheduler.java  # 자동 갱신 스케줄러
│   ├── security/
│   │   ├── JwtTokenProvider.java       # JWT 토큰 관리
│   │   ├── JwtAuthenticationFilter.java # JWT 필터
│   │   └── CustomUserDetailsService.java
│   └── service/
│       ├── AuthService.java            # 인증 서비스
│       ├── PlayerService.java          # 플레이어 서비스
│       ├── MatchService.java           # 전적 서비스
│       ├── StatsService.java           # 통계 및 랭킹 계산
│       ├── ApplicationService.java     # 신청 서비스
│       ├── PubgService.java            # PUBG API 연동
│       └── DiscordService.java         # Discord Bot 연동
└── src/main/resources/
    └── application.yml                  # 설정 파일
```

## 🚀 시작하기

### 1. 필수 요구사항

- Java 17 이상
- Gradle 8.5 이상 (Wrapper 포함)
- MongoDB 4.4 이상 (로컬 또는 MongoDB Atlas)

### 2. MongoDB 설정

#### 옵션 A: 로컬 MongoDB
```bash
# MongoDB 설치 (macOS)
brew install mongodb-community

# MongoDB 실행
brew services start mongodb-community
```

#### 옵션 B: MongoDB Atlas (추천)
1. [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) 가입
2. 무료 Cluster 생성
3. Database Access에서 사용자 생성
4. Network Access에서 IP 화이트리스트 설정 (0.0.0.0/0 또는 특정 IP)
5. 연결 문자열 복사

### 3. 환경 변수 설정

```bash
cd backend
cp .env.example .env
```

`.env` 파일 편집:
```env
PORT=5000
MONGODB_URI=mongodb://localhost:27017/cloverpit
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-min-256-bits-required-for-hs256-algorithm
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123

# PUBG API (선택사항)
PUBG_API_KEY=your-pubg-api-key
PUBG_PLATFORM=steam
PUBG_SHARD=kakao

# Discord Bot (선택사항)
DISCORD_ENABLED=false
DISCORD_BOT_TOKEN=your-discord-bot-token
DISCORD_GUILD_ID=your-discord-guild-id
DISCORD_MEMBER_ROLE_ID=your-member-role-id

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### 4. 빌드 및 실행

```bash
# 의존성 설치 및 빌드
./gradlew build

# 개발 모드 실행
./gradlew bootRun

# 또는 JAR 파일 실행
java -jar build/libs/backend-1.0.0.jar
```

서버가 http://localhost:5000 에서 실행됩니다.

## 📡 API 엔드포인트

### 인증
- `POST /api/auth/login` - 관리자 로그인
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```

### 플레이어
- `GET /api/players/rankings` - 랭킹 조회
  - Query Params: `sortBy` (score|kd|averageDamage|totalMatches), `sortOrder` (asc|desc), `limit`
- `GET /api/players/{id}` - 플레이어 상세
- `GET /api/players/{id}/matches` - 플레이어 전적
- `POST /api/players` - 플레이어 추가 (인증 필요)
- `DELETE /api/players/{id}` - 플레이어 삭제 (인증 필요)

### 전적
- `GET /api/matches/recent` - 최근 전적
  - Query Params: `limit` (기본값: 10)

### 통계
- `GET /api/stats/clan` - 클랜 통계
- `POST /api/stats/refresh` - 전적 갱신 (인증 필요)

### 가입 신청
- `GET /api/applications` - 신청 목록 조회
- `POST /api/applications` - 신청 제출
  ```json
  {
    "pubgName": "PlayerName",
    "discordName": "Discord#1234",
    "age": 25,
    "introduction": "안녕하세요..."
  }
  ```
- `PATCH /api/applications/{id}` - 신청 상태 변경 (인증 필요)

### 헬스 체크
- `GET /api/health` - 서버 상태 확인

## 🔐 인증 시스템

### JWT 토큰 사용
```bash
# 로그인
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 응답
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin"
}

# 인증이 필요한 API 호출
curl http://localhost:5000/api/players \
  -H "Authorization: Bearer {token}"
```

## 📊 랭킹 계산 공식

```java
score = (KD × 0.5) + (averageDamage / 100 × 0.3) + (log(totalMatches) × 0.2)
```

- K/D 비중: 50%
- 평균 데미지 비중: 30%
- 플레이 경험 비중: 20%

## ⏰ 자동 전적 갱신

매일 자정(00:00)에 자동으로 전체 플레이어 전적을 갱신합니다.

```java
@Scheduled(cron = "0 0 0 * * *")
public void refreshStats() {
    pubgService.refreshAllPlayersStats();
}
```

## 🎮 PUBG API 연동

### API 키 발급
1. [PUBG Developer Portal](https://developer.pubg.com/) 가입
2. API 키 생성
3. `.env` 파일에 `PUBG_API_KEY` 설정

### 더미 데이터 생성
PUBG API 키가 없는 경우, 자동으로 더미 데이터를 생성하여 테스트할 수 있습니다.

```java
// PubgService.java에서 자동으로 더미 데이터 생성
private void generateDummyStats(Player player) {
    // 5~10개의 랜덤 매치 데이터 생성
}
```

## 💬 Discord Bot 연동 (선택사항)

### Bot 설정
1. [Discord Developer Portal](https://discord.com/developers/applications) 접속
2. New Application 생성
3. Bot 탭에서 Bot 생성 및 Token 복사
4. OAuth2 → URL Generator에서 `bot` scope 선택
5. Bot Permissions에서 `Manage Roles` 선택
6. 생성된 URL로 Bot을 서버에 초대

### 환경 변수 설정
```env
DISCORD_ENABLED=true
DISCORD_BOT_TOKEN=your-bot-token
DISCORD_GUILD_ID=your-server-id
DISCORD_MEMBER_ROLE_ID=your-role-id
```

## 🔧 개발 팁

### MongoDB Compass로 데이터 확인
```bash
# MongoDB Compass 다운로드
https://www.mongodb.com/products/compass

# 연결
mongodb://localhost:27017
```

### 로그 레벨 조정
```yaml
# application.yml
logging:
  level:
    com.cloverpit: DEBUG  # DEBUG, INFO, WARN, ERROR
```

### 프로파일 사용
```bash
# 개발 환경
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 프로덕션 환경
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 📦 배포

### Railway 배포
1. [Railway](https://railway.app/) 가입
2. New Project → Deploy from GitHub
3. 환경 변수 설정
4. 자동 배포

### Render 배포
1. [Render](https://render.com/) 가입
2. New → Web Service
3. GitHub 연동
4. Build Command: `mvn clean install`
5. Start Command: `java -jar target/backend-1.0.0.jar`
6. 환경 변수 설정

## 🧪 테스트

```bash
# 단위 테스트
./gradlew test

# 통합 테스트
./gradlew integrationTest

# 특정 테스트 실행
./gradlew test --tests PlayerServiceTest

# 테스트 리포트 확인
open build/reports/tests/test/index.html
```

## 🐛 트러블슈팅

### MongoDB 연결 실패
```
com.mongodb.MongoTimeoutException: Timed out after 30000 ms
```
**해결**: MongoDB가 실행 중인지 확인, 연결 문자열 확인

### JWT 토큰 오류
```
io.jsonwebtoken.security.WeakKeyException
```
**해결**: JWT_SECRET이 256비트(32바이트) 이상인지 확인

### CORS 오류
```
Access to XMLHttpRequest has been blocked by CORS policy
```
**해결**: `CORS_ALLOWED_ORIGINS`에 프론트엔드 URL 추가

## 📝 API 문서

Swagger UI (선택사항)로 API 문서를 확인할 수 있습니다:
```
http://localhost:5000/swagger-ui.html
```

## 🤝 기여

1. Fork the Project
2. Create your Feature Branch
3. Commit your Changes
4. Push to the Branch
5. Open a Pull Request

## 📄 라이선스

MIT License

---

**개발자**: CloverPit Development Team
**문의**: [Discord 링크]
