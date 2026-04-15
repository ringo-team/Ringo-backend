# Lingo Backend

**설문 기반 매칭 · 실시간 채팅 · 커뮤니티 플랫폼 백엔드 서버**

Java 21 / Spring Boot 3.5 기반의 단일 서비스(Monolith) 애플리케이션으로, MySQL · MongoDB · Redis 멀티 데이터 스토어와 AWS S3 · Rekognition · FCM 외부 연동을 활용합니다.

---

## 목차

1. [아키텍처 개요](#1-아키텍처-개요)
2. [기술 스택](#2-기술-스택)
3. [프로젝트 구조](#3-프로젝트-구조)
4. [로컬 개발 환경 설정](#4-로컬-개발-환경-설정)
5. [환경 변수 레퍼런스](#5-환경-변수-레퍼런스)
6. [핵심 도메인 설명](#6-핵심-도메인-설명)
7. [API 문서](#7-api-문서)
8. [인프라 · 배포](#8-인프라--배포)
9. [모니터링](#9-모니터링)

---

## 1. 아키텍처 개요

```
┌──────────────────────────────────────────────────────────────────┐
│                         Client (iOS/Android)                     │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTPS / WSS
┌────────────────────────────▼─────────────────────────────────────┐
│                      Nginx (Reverse Proxy)                        │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│                    Spring Boot Application                        │
│                                                                   │
│  Presentation  →  Application (UseCase)  →  Domain / Infra       │
│                                                                   │
│  ┌──────────┐  ┌─────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  user    │  │matching │  │  chat    │  │   community      │  │
│  │  survey  │  │  snap   │  │  report  │  │   notification   │  │
│  └──────────┘  └─────────┘  └──────────┘  └──────────────────┘  │
│                                                                   │
│  shared: Security · Config · Domain Model · Infrastructure       │
└───┬──────────┬───────────┬──────────────┬────────────────────────┘
    │          │           │              │
  MySQL 8    MongoDB    Redis 6.2+     AWS S3 / Rekognition / FCM
```

**설계 방향**
- 도메인별 수직 슬라이스(Vertical Slice) 패키지 구조를 채택하여 응집도를 높입니다.
- 엔티티는 `shared/domain/model/` 한 곳에서 통합 관리합니다 (교차 바운디드 컨텍스트 참조 최소화).
- 애플리케이션 계층은 UseCase 클래스로 분리하여 컨트롤러와 도메인 로직 간 결합을 낮춥니다.
- 채팅 메시지만 MongoDB에 저장하고, 나머지 비즈니스 데이터는 MySQL에 저장합니다.

---

## 2. 기술 스택

| 범주 | 기술 |
|---|---|
| 언어 / 런타임 | Java 21 (Temurin JDK), Gradle 8.x Wrapper |
| 프레임워크 | Spring Boot 3.5.5, Spring Cloud 2025.0.0 |
| 데이터 접근 | Spring Data JPA, QueryDSL 5.0 (jakarta), Spring Data MongoDB |
| 인증 · 보안 | Spring Security, JWT (jjwt 0.12.3), OAuth2 (Google · Kakao), NICE 본인인증 |
| 실시간 | STOMP over WebSocket, SimpMessagingTemplate |
| 캐시 · 큐 | Spring Data Redis 6.2+ |
| 푸시 알림 | Firebase Admin SDK 9.7.0 (FCM) |
| 클라우드 | AWS SDK 2.25.0 (S3, Rekognition, SQS) |
| 이미지 처리 | FFmpeg 7.1.1 / JavaCV 1.5.12 (HEIC → JPG 변환) |
| 문서화 | SpringDoc OpenAPI 2.6.0 (Swagger UI) |
| 유틸리티 | Lombok, Apache POI 5.4.0 (Excel 파싱), P6Spy 3.9.1 (SQL 로깅) |
| 관측성 | Spring Actuator, Micrometer, Prometheus, Grafana |
| 배포 | Docker, AWS CodeBuild + ECR + Elastic Beanstalk |

---

## 3. 프로젝트 구조

```
lingo-backend/
├── build.gradle                    # 의존성 및 빌드 설정
├── Dockerfile                      # 컨테이너 이미지 정의 (Temurin 21)
├── docker-compose.yml              # 로컬 풀스택 (App + MySQL + Redis + Prometheus + Grafana)
├── buildspec.yml                   # AWS CodeBuild CI/CD 파이프라인
├── Dockerrun.aws.json              # Elastic Beanstalk 단일 컨테이너 배포 명세
├── prometheus/
│   └── prometheus.yml              # Prometheus 스크레이프 설정
└── src/main/
    ├── java/com/lingo/lingoproject/
    │   ├── LingoProjectApplication.java
    │   │
    │   ├── user/                   # 회원가입, 인증, 프로필, 멤버십, 휴면 계정
    │   ├── matching/               # 매칭 요청/응답, 추천 알고리즘, 스크랩
    │   ├── chat/                   # 채팅방 CRUD, STOMP 메시지, 약속 기능
    │   ├── survey/                 # 설문 업로드(Excel), 응답 저장, 일일 설문
    │   ├── community/              # 게시글, 댓글, 대댓글, 좋아요
    │   ├── snap/                   # 스냅 촬영 신청, 사진작가 프로필 관리
    │   ├── notification/           # FCM 푸시 알림, 알림 설정
    │   ├── report/                 # 신고 접수, 관리자 처리 (경고/정지/차단)
    │   │
    │   └── shared/                 # 공통 모듈
    │       ├── config/             # AWS, Redis, QueryDSL, Swagger, WebSocket, Async 설정
    │       ├── security/           # SecurityConfig, JWT 필터, OAuth2, 로그인 서비스
    │       ├── domain/model/       # 모든 JPA 엔티티 및 Enum 정의 (단일 관리)
    │       ├── exception/          # ErrorCode, RingoException, 글로벌 예외 핸들러
    │       ├── infrastructure/
    │       │   ├── persistence/    # 모든 JPA/MongoDB 리포지토리 (단일 패키지)
    │       │   │   └── impl/       # QueryDSL Custom 구현체
    │       │   └── storage/        # S3ImageStorageService (이미지 업로드/삭제/검증)
    │       ├── presentation/       # ImageController, HealthCheckController
    │       └── utils/              # GenericUtils, RedisUtils, ApiListResponseDto
    │
    └── resources/
        ├── application.yml         # 공통 설정 (파일 업로드 크기 등)
        ├── application-dev.yml     # 로컬 개발용 설정
        ├── application-prod.yml    # 프로덕션 환경 변수 바인딩
        └── static/
            └── stomp-test.html     # WebSocket/STOMP 브라우저 테스트 페이지
```

---

## 4. 로컬 개발 환경 설정

### 사전 요구사항

| 항목 | 버전 | 비고 |
|---|---|---|
| JDK | 21 (Temurin) | Gradle Toolchain이 자동 설치 |
| Docker Desktop | 최신 | MySQL · Redis 컨테이너 실행에 필요 |
| MongoDB | Atlas 또는 로컬 | 연결 URI를 환경 변수로 제공 |
| AWS 계정 | - | S3 `image-bucket-v3`, Rekognition 접근 IAM 필요 |
| Firebase 서비스 계정 | - | FCM 발송용 JSON 키 파일 필요 |

---

### Option A — Docker Compose (권장)

가장 빠르게 전체 스택을 띄우는 방법입니다.

```bash
# 1. 환경 변수 파일 생성 (아래 "환경 변수 레퍼런스" 참고)
cp .env.example .env
# .env 파일에 필수 값 입력

# 2. 전체 스택 실행 (App + MySQL + Redis + Prometheus + Grafana)
docker-compose up -d

# 3. 앱 로그 확인
docker-compose logs -f springboot
```

---

### Option B — 로컬 Gradle 빌드

인프라(MySQL · Redis · MongoDB)는 직접 실행하고, 앱만 Gradle로 띄우는 방법입니다.

```bash
# 1. MySQL 컨테이너 실행
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=ringo_mysql \
  -p 3306:3306 mysql:8.0

# 2. Redis 컨테이너 실행
docker run -d --name redis \
  -p 6379:6379 redis:6.2.6-alpine

# 3. 앱 빌드 및 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

> **참고**: `application-dev.yml`에 로컬 DB/Redis 연결 정보가 기본값으로 설정되어 있습니다.
> MongoDB URI와 AWS 자격증명은 환경 변수로 제공해야 합니다.

---

### 개발 시 유용한 엔드포인트

| 목적 | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| API 명세 (JSON) | `http://localhost:8080/v3/api-docs` |
| 헬스 체크 | `http://localhost:8080/health` |
| Prometheus 메트릭 | `http://localhost:8080/actuator/prometheus` |
| STOMP 테스트 페이지 | `http://localhost:8080/stomp` |
| Grafana 대시보드 | `http://localhost:3000` (admin / admin) |

---

## 5. 환경 변수 레퍼런스

`application-prod.yml`에서 바인딩하는 환경 변수 목록입니다.
로컬에서는 `application-dev.yml`의 기본값을 사용하되, 아래 값은 별도로 제공해야 합니다.

### 데이터베이스

| 변수명 | 설명 | 예시 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/ringo_mysql` |
| `SPRING_DATASOURCE_USERNAME` | MySQL 사용자 | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 비밀번호 | `password` |
| `SPRING_DATA_MONGODB_URI` | MongoDB 연결 URI | `mongodb+srv://user:pass@cluster0.xxx.mongodb.net/ringo_db` |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | Redis 비밀번호 | - |

### 인증 · 보안

| 변수명 | 설명 |
|---|---|
| `JWT_SECRET` | JWT 서명 비밀키 (256-bit 이상 권장) |
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 클라이언트 시크릿 |
| `KAKAO_CLIENT_ID` | 카카오 OAuth 클라이언트 ID |
| `SELF_AUTH_CLIENT_ID` | NICE 본인인증 클라이언트 ID |
| `SELF_AUTH_CLIENT_SECRET` | NICE 본인인증 클라이언트 시크릿 |

### AWS

| 변수명 | 설명 |
|---|---|
| `AWS_ACCESS_KEY` | IAM 액세스 키 (S3 · Rekognition 권한 필요) |
| `AWS_SECRET_KEY` | IAM 시크릿 키 |

### 외부 서비스

| 변수명 | 설명 |
|---|---|
| `DISCORD_WEBHOOK_URL` | 장애 알림용 Discord Webhook URL |

> **Firebase**: `GOOGLE_APPLICATION_CREDENTIALS` 환경 변수 또는 `firebase-service-account.json` 경로를 설정합니다.

---

## 6. 핵심 도메인 설명

### user — 회원 관리

회원가입 · 로그인 · 프로필 수정 · 멤버십 · 휴면 계정 처리를 담당합니다.

- **SignupUseCase**: 아이디/비밀번호 검증, 사용자 정보 저장, 친구 초대 코드 발급 및 보상 처리
- **AuthTokenUseCase**: JWT 발급 · 갱신 · 로그아웃 (Redis 블랙리스트)
- **UserQueryUseCase**: 유저 프로필 조회, 로그인 ID 찾기
- **UserUpdateUseCase**: 프로필 정보 수정, 비밀번호 재설정 (본인인증 선행 필요)
- **UserDeleteUseCase**: 회원 탈퇴 (연관 데이터 일괄 정리)
- **DormantAccountUseCase**: 휴면 계정 전환 · 복구, 접속 로그 기록
- **MembershipUseCase**: 커뮤니티 이용권 구매 및 Redis 만료 관리

### matching — 매칭 · 추천

설문 점수 기반 매칭 요청과 추천 알고리즘을 처리합니다.

- **매칭 점수 계산**: 설문 카테고리별 가중치(공간 0.2 · 자기표현 0.3 · 콘텐츠 0.25 · 공유 0.25)를 적용
- **누적 추천**: 14일 이내 활성 유저를 대상으로 설문 응답 유사도를 계산하여 추천 목록을 Redis에 캐싱
- **일일 추천**: 당일 설문 응답 패턴이 유사한 이성을 추천
- **스크랩**: 마음에 드는 상대를 저장

### chat — 실시간 채팅

STOMP over WebSocket으로 1:1 채팅을 제공합니다.

- **채팅방 생성**: 양쪽이 매칭 수락 상태여야 생성 가능
- **메시지 저장**: MongoDB에 비동기 저장
- **읽음 처리**: `readerIds` 필드로 미읽음 메시지 수 집계
- **약속 기능**: 채팅방 내 약속 일정 등록 및 알림 발송
- **WebSocket 인증**: STOMP 인터셉터에서 JWT 검증 후 채팅방 참여 여부 확인

### survey — 설문

매칭 알고리즘의 기반이 되는 설문 시스템입니다.

- 관리자가 Excel 파일로 설문 항목 업로드
- 사용자가 설문 응답 → 매칭 점수 산정에 활용
- 일일 설문: 가입 초기/랜덤 전략 혼합

### community — 커뮤니티

추천 장소(Recommendation) 기반 게시판입니다.

- 게시글 · 댓글 · 대댓글 · 좋아요 CRUD
- 이미지 업로드 시 AWS Rekognition 선정성 검사 자동 수행
- 멤버십 이용권 보유 여부에 따라 접근 제한

### snap — 스냅 촬영

사용자와 사진작가를 연결하는 예약 기능입니다.

- 스냅 촬영 신청 (날짜 · 장소 · 작가 지정)
- 작가 프로필 · 포트폴리오 이미지 등록/수정

### notification — 알림

FCM을 통한 모바일 푸시 알림을 처리합니다.

- 매칭 요청 · 채팅 · 약속 알림 자동 발송
- 알림 타입별 수신 거부 설정
- 전송 실패 시 Redis 재시도 큐 → 3회 초과 시 데드레터 저장 + Discord 알림

### shared/infrastructure/storage — 이미지 처리

`S3ImageStorageService`가 모든 이미지 관련 I/O를 담당합니다.

| 기능 | 설명 |
|---|---|
| 프로필 이미지 업로드 | 얼굴 검출 · 선정성 검사 통과 후 S3 저장 |
| 피드 이미지 업로드 | 선정성 검사 후 일괄 저장 (최대 `MAX_FEED_IMAGE_COUNT`장) |
| 얼굴 인증 (`verifyFaceIdentity`) | 프로필 사진과 신규 업로드 사진의 얼굴을 Rekognition으로 비교 |
| HEIC 변환 (`convertHeicToJpgBytes`) | FFmpeg/JavaCV로 서버 측 변환 후 JPG로 저장 |
| 비동기 삭제 (`deleteS3Object`) | `@Async`로 S3 오브젝트 삭제 |

---

## 7. API 문서

서버 실행 후 Swagger UI에서 전체 API 명세를 확인할 수 있습니다.

- **로컬**: `http://localhost:8080/swagger-ui.html`
- **프로덕션**: `https://api.ringolinkgo.com/swagger-ui.html`

### 주요 API 그룹

| 그룹 | 대표 경로 |
|---|---|
| 인증 | `POST /login`, `POST /signup`, `GET /refresh`, `POST /logout` |
| 소셜 로그인 | `GET /google/callback`, `GET /kakao/callback` |
| 본인인증 | `GET /self-auth/auth-window`, `GET /self-auth/callback` |
| 유저 | `GET /users/{id}`, `PATCH /users/{id}`, `DELETE /users/{id}` |
| 매칭 | `POST /matches`, `PATCH /matches/{id}`, `GET /users/{id}/recommendations` |
| 채팅 | `POST /chatrooms`, `GET /users/{id}/chatrooms`, `GET /chatrooms/{id}/messages` |
| 설문 | `POST /surveys`, `POST /users/{id}/surveys/responses` |
| 커뮤니티 | `POST /posts`, `GET /recommendations/{id}`, `POST /posts/{id}/comments` |
| 이미지 | `POST /profiles`, `POST /users/{id}/feed-images`, `POST /verify-profile` |
| 스냅 | `POST /snaps`, `POST /photographers/{id}` |
| 알림 | `POST /fcm/refresh`, `GET /notifications` |
| 신고 | `POST /reports` |
| 관리자 | `GET /admin/users`, `GET /admin/reports`, `GET /admin/stats` |

---

## 8. 인프라 · 배포

### CI/CD 파이프라인

```
GitHub Push
    │
    ▼
AWS CodeBuild (buildspec.yml)
    ├── ./gradlew build -x test
    ├── docker build -t ringo-app .
    ├── docker push → AWS ECR
    └── Dockerrun.aws.json → S3 업로드
                │
                ▼
        Elastic Beanstalk
        (단일 컨테이너 배포)
```

### 배포 프로파일 전환

```bash
# 프로덕션 환경에서 실행
java -Dspring.profiles.active=prod -jar lingo-project-0.0.1-SNAPSHOT.jar
```

### 데이터베이스 마이그레이션

- `dev` 프로파일: `spring.jpa.hibernate.ddl-auto: update` (자동 스키마 갱신)
- `prod` 프로파일: `spring.jpa.hibernate.ddl-auto: validate` (스키마 검증만 수행, 변경 불가)

> 스키마 변경 시 직접 DDL을 작성하여 적용해야 합니다.

---

## 9. 모니터링

| 도구 | 접근 경로 | 설명 |
|---|---|---|
| Spring Actuator | `/actuator/health`, `/actuator/info` | 헬스 체크 · 앱 정보 |
| Prometheus | `/actuator/prometheus` | JVM · HTTP · 비즈니스 메트릭 수집 |
| Grafana | `http://localhost:3000` | 메트릭 시각화 대시보드 |
| P6Spy | 콘솔 출력 (dev 프로파일) | SQL 실행 쿼리 로깅 |
| Discord Webhook | - | FCM/Discord 재시도 실패 시 알림 발송 |

---

## 기여 가이드

1. `main` 브랜치에서 feature 브랜치를 생성합니다: `git checkout -b feature/기능명`
2. 변경 사항을 커밋합니다.
3. Pull Request를 생성하고 리뷰를 요청합니다.
4. `prod` 프로파일 배포 전 반드시 로컬에서 `./gradlew build` 성공을 확인합니다.
