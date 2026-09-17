/*
 * Copyright (c) SKU LIKELION
 */
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * V20 의 해시 14,062개를 전수 검증한다. (ADR-0003 / LLD-0004)
 *
 * <p>생성 도구의 규칙 구현을 믿지 않고, export_departments.sql 이 MySQL 로 계산한 평문(TSV 4열)으로 대조한다. 앱 로그인과 같은
 * {@link BCryptPasswordEncoder#matches} 를 쓴다.
 *
 * <p>사용: {@code java -cp <spring-security-crypto.jar;commons-logging.jar> VerifyBoothManagerSeed.java
 * <departments.tsv> <V20__insert_booth_managers.sql>}
 */
public class VerifyBoothManagerSeed {

  private static final Pattern ROW =
      Pattern.compile("^\\s*ROW \\('((?:[^']|'')*)', '([A-Z]+)', '((?:[^']|'')*)', '(\\$2a\\$10\\$[./A-Za-z0-9]{53})'\\),?$");

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("usage: VerifyBoothManagerSeed <departments.tsv> <V20.sql>");
      System.exit(2);
    }
    Map<String, String> plainByKey = new HashMap<>();
    for (String line : Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8)) {
      if (line.isEmpty()) {
        continue;
      }
      String[] cols = line.split("\t", -1);
      plainByKey.put(cols[0] + "\t" + cols[1] + "\t" + cols[2], cols[3]);
    }

    List<String[]> rows =
        Files.readAllLines(Path.of(args[1]), StandardCharsets.UTF_8).stream()
            .filter(l -> l.stripLeading().startsWith("ROW ("))
            .map(
                l -> {
                  Matcher m = ROW.matcher(l);
                  if (!m.matches()) {
                    throw new IllegalStateException("형식이 다른 행: " + l);
                  }
                  return new String[] {
                    m.group(1).replace("''", "'"), m.group(2), m.group(3).replace("''", "'"), m.group(4)
                  };
                })
            .toList();

    if (rows.size() != plainByKey.size()) {
      fail("행 수 불일치: V20=" + rows.size() + ", TSV=" + plainByKey.size());
    }

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    AtomicInteger failures = new AtomicInteger();
    long start = System.nanoTime();
    rows.parallelStream()
        .forEach(
            r -> {
              String plain = plainByKey.get(r[0] + "\t" + r[1] + "\t" + r[2]);
              if (plain == null || !encoder.matches(plain, r[3])) {
                failures.incrementAndGet();
                System.err.println("불일치: " + String.join(" / ", r[0], r[1], r[2]));
              }
            });
    double seconds = (System.nanoTime() - start) / 1e9;

    // 음성 검사: 평문을 한 글자만 바꾸면 통과하지 않아야 한다
    String[] sample = rows.get(0);
    String samplePlain = plainByKey.get(sample[0] + "\t" + sample[1] + "\t" + sample[2]);
    boolean negativeRejected = !encoder.matches(samplePlain + "x", sample[3]);

    System.out.printf(
        "rows=%d, matches_ok=%d, failures=%d, negative_rejected=%s, %.1fs%n",
        rows.size(), rows.size() - failures.get(), failures.get(), negativeRejected, seconds);
    if (failures.get() > 0 || !negativeRejected) {
      System.exit(1);
    }
  }

  static void fail(String message) {
    System.err.println("FAIL: " + message);
    System.exit(1);
  }
}
