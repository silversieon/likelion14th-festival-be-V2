# 부스 매니저 적재 도구

`V20__insert_booth_managers.sql`(서경대학교 제외 전국 학과 14,062개의 `BOOTH_MANAGER` 계정)을 **만들고 검증하는** 일회성 도구다.
근거: [ADR-0003](../../../docs/adr/ADR-0003-booth-manager-seed-with-precomputed-bcrypt.md) / [LLD-0004](../../../docs/lld/LLD-0004-booth-manager-seed.md)

> ⚠️ V20은 이미 생성·커밋되어 있다. **다시 실행해 V20을 덮어쓰지 않는다.** BCrypt salt가 무작위라 해시가 모두 바뀌고,
> 이미 적용된 DB에서는 체크섬 불일치로 앱이 기동하지 않는다 (policy 4.1). 이 도구는 기록·재현용이다.

## 비밀번호 규칙 (로그인할 때 필요)

```
학교명에서 공백을 모두 뺀 앞 최대 5글자 + "_" + region + "_" + 학과명에서 공백을 모두 뺀 앞 최대 5글자
```

| 학교 / 학과 (region) | 비밀번호 |
|---|---|
| 서강대학교 / 컴퓨터공학과 (SEOUL) | `서강대학교_SEOUL_컴퓨터공학` |
| 강원대학교 / 경영대학 무전공학과 (GANGWON) | `강원대학교_GANGWON_경영대학무` |

MySQL로 구하기:

```sql
SELECT u.id AS universityId, d.id AS departmentId,
       CONCAT(LEFT(REPLACE(u.name, ' ', ''), 5), '_', u.region, '_', LEFT(REPLACE(d.name, ' ', ''), 5)) AS password
FROM departments d JOIN universities u ON u.id = d.university_id
WHERE u.name = '서강대학교';
```

## 실행 (저장소 루트, Git Bash 기준)

선행 조건: 로컬 MySQL(`mysql` 컨테이너)에 V16까지 적용, Java 21, Gradle 캐시에 의존성이 받아져 있을 것 (`./gradlew build` 한 번).

```bash
# 0) classpath — 앱과 같은 BCrypt 구현 (버전은 ./gradlew dependencies 로 확인한 것과 맞춘다)
GRADLE_CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
CRYPTO=$(find "$GRADLE_CACHE/org.springframework.security/spring-security-crypto" -name "spring-security-crypto-7.0.4.jar")
LOGGING=$(find "$GRADLE_CACHE/commons-logging/commons-logging" -name "commons-logging-*.jar" ! -name "*sources*" | sort | tail -1)
CP="$CRYPTO;$LOGGING"   # Windows 구분자 ; (macOS·Linux 는 :)

# 1) 대상 추출 (탭 구분: 학교명, region, 학과명, MySQL 계산 평문)
docker exec -i mysql mysql --default-character-set=utf8mb4 -N -B -uroot -p1234 festival2 \
  < scripts/seed/booth-manager/export_departments.sql > departments.tsv

# 2) 생성 — Java 규칙이 MySQL 평문과 한 건이라도 다르면 중단
java -cp "$CP" scripts/seed/booth-manager/GenerateBoothManagerSeed.java \
  departments.tsv src/main/resources/db/migration/V20__insert_booth_managers.sql

# 3) 전수 검증 — 해시 14,062개를 MySQL 평문으로 matches
java -cp "$CP" scripts/seed/booth-manager/VerifyBoothManagerSeed.java \
  departments.tsv src/main/resources/db/migration/V20__insert_booth_managers.sql
```

`departments.tsv`는 커밋하지 않는다 (규칙으로 다시 만들 수 있다).
