# 🦁 LION ORDER — 대학 축제 QR 오더 서비스 v2

> 축제에서 실제로 운영한 QR 주문 서비스(v1)를 바탕으로,
> **장애와 트래픽 상황에서도 주문 정합성을 보장하도록 재설계한 백엔드**입니다.

![image](https://github.com/user-attachments/assets/a38799f3-bb5a-4a81-96fc-c12d109da3e6)

| 항목 | 내용 |
|---|---|
| v1 운영 규모 | 사용자 약 **4,800명**, 주문 약 **2,300건** (멋쟁이사자처럼 서경대 축제) |
| 담당 | 백엔드 · 주문 도메인 |
| 기간 | 2026.07 ~ 2026.08 (v2 리팩토링 / 개인 프로젝트) |
| v1 저장소 | https://github.com/silversieon/likelion14th-festival-be |

<br>

## 📌 목차

1. [아키텍처](#-아키텍처)
2. [기술 스택](#-기술-스택)
3. [결과 요약](#-결과-요약)
4. [문제 해결 과정](#-문제-해결-과정)
   - [1. 키 선점 시 커넥션 중첩 점유로 인한 풀 고갈](#1-키-선점-시-커넥션-중첩-점유로-인한-풀-고갈)
   - [2. Redis 장애 시 멱등성 보장 불가](#2-redis-장애-시-멱등성-보장-불가)
   - [3. 주문 알림 발행 과정 유실](#3-주문-알림-발행-과정-유실)
   - [4. 주문 알림 전달 과정 유실](#4-주문-알림-전달-과정-유실)
5. [진행 중인 작업](#-진행-중인-작업)
6. [로컬 실행](#-로컬-실행)

<br>

## 🏗 아키텍처

![image](https://github.com/user-attachments/assets/c23969bf-2d07-4111-9b89-6468522a2bcf)

- **Nginx** 뒤에 애플리케이션 인스턴스 **3대**를 둔 분산 구성
- 주문 멱등성: **Redis 키 선점 + Resilience4j 서킷 브레이커 + Write-Through(MySQL)**
- 주문 알림: **트랜잭셔널 아웃박스 → Redis Streams(Consumer Group) → SSE (Last-Event-ID 재전송)**
- 모니터링: **Prometheus + Grafana**, 부하 테스트: **Gatling**

<br>

## 🛠 기술 스택

| 구분 | 기술 |
|---|---|
| Language / Framework | Java 21, Spring Boot 4.0.5, Hibernate 7 |
| Database / Cache | MySQL 8.0, Redis 7 (Streams) |
| Resilience | Resilience4j (Retry, Circuit Breaker) |
| Infra | Docker, Nginx |
| Monitoring / Test | Prometheus, Grafana, Gatling, Testcontainers |

<br>

## 📊 결과 요약

| 문제 | 해결 | 결과 |
|---|---|---|
| 키 선점 시 커넥션 중첩 점유 → 풀 고갈 | 트랜잭션 분리 + Redis 키 선점 | 요청당 커넥션 획득 **2회 → 1회**, 쓰루풋 **약 50% 증가**, 레이턴시 **약 55% 감소** |
| Redis 장애 시 멱등성 보장 불가 | Retry · 서킷 브레이커 + Write-Through | Redis 장애 주입 환경에서 피크 시나리오 **6,600건 성공, 중복 주문 0건** |
| 주문 알림 발행 과정 유실 | 트랜잭셔널 아웃박스 + `SKIP LOCKED` | 커밋된 주문은 반드시 이벤트가 남고, 장애 후 **자동 재발행** |
| 주문 알림 전달 과정 유실 | Redis Streams + Last-Event-ID 재전송 | 재연결 시 **끊긴 구간의 알림 재전송** |

- 추가로, 멱등성 로직을 **AOP + 전략 패턴**으로 분리해 주문 외 기능에도 재사용 가능한 구조로 개선했습니다.

<br>

## 🔥 문제 해결 과정

### 목표 설정 — 전국 대학 규모의 피크 트래픽

| 항목 | 기준 |
|---|---|
| 목표 | **피크 22 QPS** 처리 (v1 피크 0.066 QPS × 전국 대학 수 330) |
| 피크 QPS 산출 | v1 피크 시간대(18~19시) 주문 722건 / 3일 / 3,600초 ≈ 0.066 QPS |
| 수용 기준 | **중복 주문 발생률 0%, 정상 요청 성공률 100%** |

---

### 1. 키 선점 시 커넥션 중첩 점유로 인한 풀 고갈

#### 문제

목표 피크 수치(22 QPS) 수준으로 주문 시나리오를 돌리자 **22건 중 11건만 성공, 절반이 실패**했습니다.

- 테스트 시나리오: 정상 요청 18개 + 멱등 요청 4개(최초 요청 2개 + 중복 재시도 2개)

![image](https://github.com/user-attachments/assets/4d46cc48-a4cc-4aff-ae78-9eaacb4fc532)

```
Caused by: org.hibernate.exception.JDBCConnectionException:
Unable to acquire JDBC Connection
[HikariPool-1 - Connection is not available, ...
```

에러 로그에서 **HikariCP 커넥션 획득 실패**를 확인 → 동시 요청 수에 비해 커넥션 풀이 부족하다고 판단하고, **요청당 커넥션 점유 구조**를 분석했습니다.

#### 원인

![image](https://github.com/user-attachments/assets/98cd9f23-1cd8-4a70-aee0-3df05d84c344)

주문 트랜잭션 안에서 멱등키 선점을 `REQUIRES_NEW`로 처리해, **요청 하나가 커넥션 2개를 동시에 점유**했습니다.
부하가 걸리면 풀이 고갈되며 타임아웃 실패가 발생했습니다.

#### 해결 (1) — 키 선점 트랜잭션과 주문 저장 트랜잭션 분리

![image](https://github.com/user-attachments/assets/b0eacc1d-1892-4868-ae83-0c115c6bfe9b)

- 키 선점 트랜잭션과 주문 저장 트랜잭션을 **중첩 없이 분리**
- 커넥션이 필요한 구간에서만 획득 후 바로 반납하도록 구성

![image](https://github.com/user-attachments/assets/0ac86a9f-9267-483e-824f-488b240cbdcd)

| | 내용 |
|---|---|
| ✅ 결과 | 목표 피크 수치 주문 시나리오 **22/22 성공** |
| 👍 장점 | 추가 인프라 없이 하나의 DBMS로 멱등성 보장, 정합성 관리 용이 |
| ⚠️ 한계 | 비즈니스와 무관한 데이터를 DB에서 관리. 동시 중첩은 제거했지만 **요청 1회에 커넥션 획득 2번은 여전함** |

#### 해결 (2) — 키 선점을 Redis로 이동

![image](https://github.com/user-attachments/assets/3331296d-4446-462e-9d8e-5663d2f241e9)

키 선점을 **Redis `SET NX`** 원자 연산으로 처리해, DB 커넥션 없이 키를 선점하도록 변경했습니다.

![image](https://github.com/user-attachments/assets/e0c07bcb-3769-4953-8639-a3b9607d6cb5)
![image](https://github.com/user-attachments/assets/24a15d78-20ad-41a3-ba33-429cb3449016)

| 지표 | MySQL 키 선점 | Redis 키 선점 |
|---|---|---|
| 요청당 커넥션 획득 | 2회 | **1회** |
| 쓰루풋 | 기준 | **약 50% 증가** |
| 레이턴시 | 기준 | **약 55% 감소** |

#### 추가 설계 고려 사항

- 멱등키 삽입 시 정렬 보장 및 **InnoDB 페이지 스플릿 방지**를 위해 PK를 **UUIDv7**로 설계
- MySQL은 **유니크 제약**, Redis는 **`SET NX`** 로 원자적 키 선점
- 주문 저장 실패 시, 이미 커밋된 선점 키는 **보상 트랜잭션**으로 해제

---

### 2. Redis 장애 시 멱등성 보장 불가

#### 요구사항

키 선점 **이전 · 도중 · 이후** 어느 시점에 Redis 장애가 나도 주문 요청의 멱등성을 보장해야 합니다.

- 수용 기준: 최대 **100 users/s 램프업(6,600건)** + 전량 중복 요청 + **Redis 3회 장애 주입** → **에러 0건, 중복 주문 0건**

![image](https://github.com/user-attachments/assets/550e02c0-752e-4eab-a8b0-5b740b34a8bf)

#### 문제 — 서킷 브레이커 없는 try-catch fallback

![image](https://github.com/user-attachments/assets/82457308-2da3-4a1a-802d-673c3940db28)

1. 모든 요청이 **Redis 타임아웃을 거친 뒤** fallback → 응답 지연 누적
2. Redis 복구 순간 **트래픽 집중** → Thundering Herd

#### 해결 (1) — Resilience4j Retry + 서킷 브레이커

![image](https://github.com/user-attachments/assets/63e92939-3eab-46f3-ac74-af06f6101e50)

- **Retry**로 일시 장애 흡수, **서킷 브레이커**로 지속 장애 시 즉시 DB 우회
- **Half-Open** 상태로 점진 복구 → Thundering Herd 해소
- 단, 서킷 브레이커는 지연만 차단할 뿐 정합성은 보장하지 않으므로 **중복 방지의 최종 책임은 DB 유니크 제약**이 담당

![image](https://github.com/user-attachments/assets/6ac77618-1746-4b1f-b325-0b692b4d9bb7)

**추가 발생 문제**: 성공률 **99.91%(6,594/6,600)** 였지만 재시도 요청에서 **중복 주문 발생**

- 원인: **최초 요청과 재시도가 서로 다른 저장소를 참조** → 멱등키 미발견 → 신규 요청으로 처리
- 즉, 키 선점 이전·도중 장애는 해소됐지만 **선점 이후 장애는 멱등성 보장 불가**

#### 해결 (2) — 멱등키 선점에 Write-Through 적용

![image](https://github.com/user-attachments/assets/e9bafae4-0f18-4386-8ba3-25d2e08d6440)

멱등키를 **Redis와 DB에 동시 기록**해, 서킷 전환으로 참조 저장소가 바뀌어도 키를 찾을 수 있도록 했습니다.

![image](https://github.com/user-attachments/assets/97aba809-f6e3-48c3-b4b5-3a5c5df5c033)

| | 결과 |
|---|---|
| 성공률 | **100% (6,600/6,600)** |
| 중복 재시도 | **3,300건 모두 멱등 처리** |
| 중복 주문 | **0건** |

#### 추가 설계 고려 사항

- 멱등성 로직을 주문 도메인에서 분리하기 위해 **AOP 기반 커스텀 어노테이션(`@Idempotent`)** 으로 메서드 단위 적용
- 요구사항·인프라 환경에 따라 저장소 전략을 교체할 수 있도록 **전략 패턴** 적용 (`DB` / `REDIS` / `WRITETHROUGH` / `FALLBACK`)

```java
@Idempotent(idempotencyKey = "#idempotencyKey", strategy = IdempotencyType.FALLBACK)
public OrderResponse createOrder(Long boothId, String idempotencyKey, OrderCreateRequest request) { ... }
```

> 📂 `global/util/idempotency` — `IdempotencyAspect`, `IdempotencyStrategyFactory`, `strategy/{db,redis,writethrough,fallback}`

---

### 3. 주문 알림 발행 과정 유실

#### 요구사항

장애가 발생해도 **주문 알림은 반드시 부스 관리자에게 전달**되어야 합니다.

#### 기존 구조

![image](https://github.com/user-attachments/assets/1613779f-d6a8-463e-9c54-710a61f6eb8b)

주문 커밋 후 트랜잭션 이벤트 리스너가 비동기로 SSE Service에 전달해 관리자에게 실시간 푸시 → 주문 응답과 알림을 분리.

#### 문제

![image](https://github.com/user-attachments/assets/2cb4c8be-4470-45c2-b6a9-ba34fd66a008)

- **장애 상황 (1) 이벤트 발행**: 커밋 후 SSE Service 처리 중 장애 시 **메모리상 이벤트 유실**
- **장애 상황 (2) 이벤트 전달**: 관리자 연결 끊김 · 네트워크 장애 시 **전송 실패 이벤트 유실** → [4번](#4-주문-알림-전달-과정-유실)에서 해결

#### 해결 — 트랜잭셔널 아웃박스 패턴 + `SKIP LOCKED`

![image](https://github.com/user-attachments/assets/77568c5c-2e67-48e7-8255-7d9ebc04502e)
![image](https://github.com/user-attachments/assets/a98363cc-de38-46de-b8c2-78941eca2839)

1. 주문과 아웃박스 이벤트를 **동일 트랜잭션에 저장** → 커밋된 주문에는 반드시 이벤트가 남음
2. Publisher가 **200ms마다** 미발행 이벤트를 조회·발행하고, **전송 성공 시에만 완료 처리** → 발행 중 장애가 나도 재기동 후 자동 재발행
3. 미발행 이벤트 조회 시 **`FOR UPDATE SKIP LOCKED`** 적용 → 다중 인스턴스에서도 각 이벤트를 한 인스턴스만 처리해 중복 발행 방지

| | 내용 |
|---|---|
| 👍 장점 | 별도 메시지 브로커 없이 기존 DB만으로 이벤트 발행 보장 |
| ⚠️ 한계 | 폴링 주기만큼의 알림 지연과 DB 조회 부하. 전송 성공 후 완료 처리 전 장애 시 중복 발행 가능 |
| 🔜 개선 방향 | 이벤트 ID 기반 수신 측 중복 제거, CDC 도입으로 폴링 부하 해소 고려 |

> 📂 `global/outbox`, `domain/order/event` — `order.event.pattern = direct | outbox`

---

### 4. 주문 알림 전달 과정 유실

#### 해결 — Redis Streams + Last-Event-ID 재전송

![image](https://github.com/user-attachments/assets/7fef1eab-e985-472b-8027-1a4ecd998559)

**왜 Pub/Sub이 아니라 Streams인가**

| | Pub/Sub | Streams |
|---|:---:|:---:|
| 메시지 보관 | ❌ | ✅ |
| ID 기반 재조회 | ❌ | ✅ |
| Consumer Group | ❌ | ✅ |

1. **인스턴스별 Consumer Group**으로 모든 인스턴스가 이벤트를 수신 → 관리자가 **어느 인스턴스에 연결돼 있어도** 알림 전달
2. **Stream 메시지 ID를 SSE 이벤트 ID로 사용** → 재연결 시 `Last-Event-ID` 이후 이벤트를 재전송해 **연결이 끊긴 사이의 알림 유실 방지**

| | 내용 |
|---|---|
| 👍 장점 | 이미 사용 중인 Redis로 별도 메시지 브로커 없이 구현 |
| ⚠️ 한계 | Stream이 계속 쌓여 보관 정책 필요, 보관 범위를 벗어난 뒤 재연결하면 재전송 불가. 인스턴스 수만큼 같은 이벤트를 중복 소비 |
| 🔜 개선 방향 | `MAXLEN` 기반 보관 정책 적용, 트래픽 증가 시 Kafka 전환 고려 |

> 📂 `domain/order/sse/redis/stream` — `sse.strategy = local | distributed`, `sse.redis.mode = pubsub | stream`

<br>

## 🚧 진행 중인 작업

v2의 다음 단계로, **전국 단위 대학 서비스 확장**과 **대규모 데이터 환경에서의 성능 고도화**를 진행하고 있습니다.

| 작업 | 내용 | 상태 |
|---|---|---|
| 전국 대학 확장 | 대학 ↔ 학과(운영 주체) ↔ 부스 연관관계 스키마 개편 | 🔄 진행 중 |
| 대량 데이터 적재 | 수십만 → 수백만 → 수천만 건 단계적 시드 데이터 적재 | 🔄 진행 중 |
| 대용량 조회 성능 개선 | 인덱스 · 쿼리 튜닝 · 캐싱 (Before/After 실측 기반) | ⏳ 예정 |
| Read Replica / CQRS | 읽기 부하 분산, MongoDB Read Model 설계 | ⏳ 예정 |
| AI 활용 | <!-- TODO: AI 활용 내용 (예: 부스·메뉴 다국어 번역 등) --> | 🔄 진행 중 |

> 설계 결정과 상세 설계는 [`docs/adr`](docs/adr) · [`docs/lld`](docs/lld)에 기록하고 있습니다.

<br>

## 🚀 로컬 실행

Nginx 뒤에 앱 3대(`festival-app-01|02|03`)가 붙은 분산 구성으로 실행됩니다. (Docker Desktop 필요)

```bash
docker network create festival-network   # 최초 1회

./start.sh        # macOS / Linux
.\start.bat       # Windows

docker ps -a --filter name=nginx   # nginx가 Exited면 `docker start nginx`
```

| 대상 | 접속 |
|---|---|
| Nginx (진입점) | http://localhost:8888 |
| App #1 / #2 / #3 | http://localhost:8080 / 8081 / 8082 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

```bash
./gradlew test    # 통합 테스트는 Testcontainers(MySQL, Redis) 사용
```
