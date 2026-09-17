/*
 * Copyright (c) SKU LIKELION
 */
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 전국 부스 매니저 적재용 Flyway 마이그레이션(V20)을 만든다. (ADR-0003 / LLD-0004)
 *
 * <p>비밀번호 평문은 규칙 ④로 만들고, 앱 SecurityConfig 와 같은 {@link BCryptPasswordEncoder}(strength 10)로
 * 해시한다. 파일에는 해시만 들어간다.
 *
 * <p>사용: {@code java -cp <spring-security-crypto.jar;commons-logging.jar> GenerateBoothManagerSeed.java
 * <departments.tsv> <V20__insert_booth_managers.sql>}
 */
public class GenerateBoothManagerSeed {

  private static final int BCRYPT_STRENGTH = 10;
  private static final int PREFIX_MAX_CODE_POINTS = 5;
  private static final int BCRYPT_MAX_BYTES = 72;
  private static final String EXCLUDED_UNIVERSITY = "서경대학교";
  private static final String EXCLUDED_REGION = "SEOUL";

  record Department(String universityName, String region, String departmentName, String mysqlPlain) {}

  /** 공백(U+0020)을 모두 뺀 뒤 앞 최대 5글자(코드 포인트 단위). MySQL LEFT(REPLACE(name, ' ', ''), 5) 와 같다. */
  static String prefix(String name) {
    String withoutSpaces = name.replace(" ", "");
    return withoutSpaces
        .codePoints()
        .limit(PREFIX_MAX_CODE_POINTS)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

  static String plainPassword(String universityName, String region, String departmentName) {
    return prefix(universityName) + "_" + region + "_" + prefix(departmentName);
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("usage: GenerateBoothManagerSeed <departments.tsv> <output.sql>");
      System.exit(2);
    }
    List<Department> departments = readTsv(Path.of(args[0]));
    validate(departments);

    long start = System.nanoTime();
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    // 병렬로 계산하되 List 순서(입력 순서)는 유지된다
    List<String> hashes =
        departments.parallelStream()
            .map(d -> encoder.encode(d.mysqlPlain()))
            .collect(Collectors.toList());
    double seconds = (System.nanoTime() - start) / 1e9;

    Files.writeString(Path.of(args[1]), buildSql(departments, hashes), StandardCharsets.UTF_8);
    System.out.printf(
        "rows=%d, hash=%.1fs (%d threads), output=%s (%d bytes)%n",
        departments.size(),
        seconds,
        Runtime.getRuntime().availableProcessors(),
        args[1],
        Files.size(Path.of(args[1])));
  }

  static List<Department> readTsv(Path path) throws IOException {
    List<Department> result = new ArrayList<>();
    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      if (line.isEmpty()) {
        continue;
      }
      String[] cols = line.split("\t", -1);
      if (cols.length != 4) {
        fail("TSV 열 수가 4가 아니다: " + line);
      }
      result.add(new Department(cols[0], cols[1], cols[2], cols[3]));
    }
    return result;
  }

  static void validate(List<Department> departments) {
    int mismatches = 0;
    for (Department d : departments) {
      if (d.universityName().equals(EXCLUDED_UNIVERSITY) && d.region().equals(EXCLUDED_REGION)) {
        fail("제외 대상인 서경대학교 행이 입력에 있다: " + d);
      }
      String javaPlain = plainPassword(d.universityName(), d.region(), d.departmentName());
      if (!javaPlain.equals(d.mysqlPlain())) {
        mismatches++;
        System.err.printf("규칙 불일치: java=[%s] mysql=[%s] (%s)%n", javaPlain, d.mysqlPlain(), d);
      }
      if (javaPlain.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
        fail("BCrypt 72바이트 초과: " + d);
      }
    }
    if (mismatches > 0) {
      fail("Java 규칙과 MySQL 규칙이 다른 행: " + mismatches);
    }
  }

  static String buildSql(List<Department> departments, List<String> hashes) {
    StringBuilder sql = new StringBuilder();
    sql.append(
        """
        -- ===================================
        -- V20__insert_booth_managers.sql
        -- 서경대학교를 제외한 전국 학과마다 부스 매니저 계정을 1개씩 적재한다 (role = BOOTH_MANAGER).
        --
        -- 근거: ADR-0003 (승인 2026-09-17, 옵션 B) / LLD-0004 / 이슈 #14
        -- 예상 건수: %d
        --
        -- 생성: scripts/seed/booth-manager/GenerateBoothManagerSeed.java 로 만든 파일이다. 손으로 고치지 않는다.
        --       salt 가 무작위라 다시 생성하면 해시가 모두 바뀐다 → 적용 후 수정 금지 (policy 4.1).
        --
        -- password: 아래 평문 규칙 ④를 BCrypt(cost 10, $2a$)로 해시한 값. 평문은 파일에 넣지 않는다.
        --   평문 = LEFT(REPLACE(학교명, ' ', ''), 5) + '_' + region + '_' + LEFT(REPLACE(학과명, ' ', ''), 5)
        --   예: 서강대학교 / 컴퓨터공학과 (SEOUL)     -> 서강대학교_SEOUL_컴퓨터공학
        --       강원대학교 / 경영대학 무전공학과 (GANGWON) -> 강원대학교_GANGWON_경영대학무
        --   서로 다른 계정이 같은 평문을 가질 수 있다 (수용 — 로그인은 대학·학과 id로 계정을 먼저 찾는다).
        --
        -- 학교·학과는 id 가 아니라 이름으로 찾는다 (V16 관례 — 환경마다 id 가 다를 수 있다).
        -- LEFT JOIN: 못 찾으면 department_id 가 NULL 이 되어 NOT NULL 제약으로 마이그레이션이 실패한다.
        -- ===================================

        INSERT INTO managers (department_id, password, role, created_at, modified_at)
        SELECT d.id, v.password, 'BOOTH_MANAGER', NOW(6), NOW(6)
        FROM (VALUES
        """
            .formatted(departments.size()));
    for (int i = 0; i < departments.size(); i++) {
      Department d = departments.get(i);
      sql.append("    ROW (")
          .append(quote(d.universityName()))
          .append(", ")
          .append(quote(d.region()))
          .append(", ")
          .append(quote(d.departmentName()))
          .append(", ")
          .append(quote(hashes.get(i)))
          .append(i == departments.size() - 1 ? ")\n" : "),\n");
    }
    sql.append(
        """
        ) AS v (university_name, university_region, department_name, password)
        LEFT JOIN universities u ON u.name = v.university_name AND u.region = v.university_region
        LEFT JOIN departments d ON d.university_id = u.id AND d.name = v.department_name;
        """);
    return sql.toString();
  }

  /** MySQL 문자열 리터럴. 백슬래시와 작은따옴표를 이스케이프한다 (현재 데이터에는 둘 다 없다). */
  static String quote(String value) {
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
  }

  static void fail(String message) {
    System.err.println("FAIL: " + message);
    System.exit(1);
  }
}
