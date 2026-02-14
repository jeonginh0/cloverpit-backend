# CloverPit Backend - 빠른 시작 가이드

## 🚀 5분 안에 시작하기

### 1단계: 환경 설정

```bash
cd backend

# .env 파일 생성
cp .env.example .env
```

### 2단계: MongoDB 준비

**옵션 A: Docker로 빠르게 시작**
```bash
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

**옵션 B: MongoDB Atlas (무료)**
1. https://www.mongodb.com/cloud/atlas 가입
2. 무료 클러스터 생성
3. 연결 문자열 복사
4. `.env` 파일에 `MONGODB_URI` 설정

### 3단계: 실행

```bash
# Gradle로 실행
./gradlew bootRun
```

서버가 http://localhost:5000 에서 실행됩니다!

---

## 📌 기본 관리자 계정

- **Username**: `admin`
- **Password**: `admin123`

(`.env` 파일에서 변경 가능)

---

## 🧪 API 테스트

### 1. 헬스 체크
```bash
curl http://localhost:5000/api/health
```

### 2. 관리자 로그인
```bash
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 3. 플레이어 추가
```bash
# 먼저 로그인에서 받은 token을 사용
TOKEN="your-jwt-token-here"

curl -X POST http://localhost:5000/api/players \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "pubgName": "TestPlayer",
    "discordName": "Discord#1234"
  }'
```

### 4. 전적 갱신 (더미 데이터 생성)
```bash
curl -X POST http://localhost:5000/api/stats/refresh \
  -H "Authorization: Bearer $TOKEN"
```

### 5. 랭킹 조회
```bash
curl http://localhost:5000/api/players/rankings
```

### 6. 클랜 통계 조회
```bash
curl http://localhost:5000/api/stats/clan
```

---

## 🎯 프론트엔드 연동

### 1. 프론트엔드 .env 설정
```bash
cd ../frontend
echo "NEXT_PUBLIC_API_URL=http://localhost:5000/api" > .env.local
```

### 2. 프론트엔드 실행
```bash
npm run dev
```

### 3. 브라우저 접속
```
http://localhost:3000
```

---

## 🔧 개발 모드 설정

### 자동 재시작 (DevTools)
```xml
<!-- pom.xml에 이미 포함됨 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

파일 수정 시 자동으로 재시작됩니다.

### 로그 레벨 변경
```yaml
# application.yml
logging:
  level:
    com.cloverpit: DEBUG
```

---

## 💡 자주 사용하는 명령어

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 클린 빌드
./gradlew clean build

# 실행
./gradlew bootRun

# JAR 파일 실행
java -jar build/libs/backend-1.0.0.jar

# 특정 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

---

## 🐛 문제 해결

### MongoDB 연결 안 됨
```bash
# MongoDB 상태 확인
brew services list | grep mongodb

# MongoDB 시작
brew services start mongodb-community

# 또는 Docker
docker ps | grep mongodb
```

### 포트 이미 사용 중
```bash
# 5000 포트 사용 프로세스 확인
lsof -i :5000

# 프로세스 종료
kill -9 <PID>
```

### 빌드 실패
```bash
# Gradle 캐시 삭제
./gradlew clean

# 의존성 다시 다운로드
./gradlew build --refresh-dependencies
```

---

## 📊 데이터 확인

### MongoDB 데이터 확인
```bash
# MongoDB Shell 접속
mongosh

# 데이터베이스 선택
use cloverpit

# 플레이어 목록
db.players.find()

# 전적 목록
db.matches.find()

# 관리자 목록
db.admins.find()
```

---

## 🎮 더미 데이터 생성

```bash
# 1. 플레이어 추가
curl -X POST http://localhost:5000/api/players \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pubgName":"Player1","discordName":"Discord1#1234"}'

curl -X POST http://localhost:5000/api/players \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pubgName":"Player2","discordName":"Discord2#1234"}'

# 2. 전적 갱신으로 더미 데이터 생성
curl -X POST http://localhost:5000/api/stats/refresh \
  -H "Authorization: Bearer $TOKEN"

# 3. 결과 확인
curl http://localhost:5000/api/players/rankings
```

---

## ✅ 체크리스트

- [ ] Java 17 설치 확인: `java -version`
- [ ] Maven 설치 확인: `mvn -version`
- [ ] MongoDB 실행 확인
- [ ] `.env` 파일 설정
- [ ] 백엔드 실행: `mvn spring-boot:run`
- [ ] 헬스 체크: `curl http://localhost:5000/api/health`
- [ ] 로그인 테스트
- [ ] 프론트엔드 연동

---

**🎉 설정 완료! 이제 개발을 시작하세요!**
