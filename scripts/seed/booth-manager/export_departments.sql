-- 부스 매니저 적재 대상 추출 (ADR-0003 / LLD-0004)
--
-- 출력: 탭 구분, 헤더 없음 — 학교명, region, 학과명, 비밀번호 평문(MySQL 계산)
-- 대상: 서경대학교를 제외한 모든 학과 (서경대는 매니저 33명이 이미 있다)
-- 정렬: V17~V19 의 seq 와 같은 (학교명, region, 학과명)
--
-- 4번째 열이 비밀번호 규칙 ④의 기준 값이다.
--   공백(U+0020)을 모두 뺀 이름의 앞 최대 5글자 + "_" + region + "_" + 같은 방식의 학과명
-- 생성 도구는 자기 계산 결과가 이 값과 다르면 중단한다.

SELECT u.name,
       u.region,
       d.name,
       CONCAT(LEFT(REPLACE(u.name, ' ', ''), 5), '_', u.region, '_', LEFT(REPLACE(d.name, ' ', ''), 5))
FROM departments d
         JOIN universities u ON u.id = d.university_id
WHERE NOT (u.name = '서경대학교' AND u.region = 'SEOUL')
ORDER BY u.name, u.region, d.name;
