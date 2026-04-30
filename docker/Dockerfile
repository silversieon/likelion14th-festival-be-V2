# ============================================
# Stage 1: 빌드 스테이지
# ============================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Gradle 래퍼와 의존성 파일만 먼저 복사 (캐싱 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 (이 레이어는 build.gradle 변경 시만 재실행)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY src src

# 프로젝트 빌드
RUN ./gradlew build -x test --no-daemon

# ============================================
# Stage 2: 런타임 스테이지
# ============================================
FROM eclipse-temurin:21-jre

# 타임존 설정 및 필수 패키지 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        tzdata && \
    rm -rf /var/lib/apt/lists/* && \
    ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# 비root 사용자 생성 (보안)
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# 빌드 이미지에서 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 소유권 변경
RUN chown -R spring:spring /app

# 사용자 전환
USER spring

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행 (환경변수로 JVM 옵션 받음)
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]