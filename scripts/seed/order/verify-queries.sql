-- =====================================================================================
-- 주문 대량 적재 조건 — 수동 확인용 SQL (LLD-0006 9.2, 13.2)
--
-- verify.py가 같은 내용을 자동으로 판정한다. 이 파일은 사람이 직접 눈으로 확인하거나
-- 발표 자료용 표를 뽑을 때 쓴다. 쿼리 하나씩 실행한다.
--
--   docker exec -i mysql mysql -uroot -p1234 festival2 < verify-queries.sql      # 전부 실행
--   (또는 DataGrip/Workbench 등에서 localhost:3307, festival2 로 접속해 한 블록씩 실행)
--
-- 고정값 (fixtures/ 기준):
--   HOT 학교 = 1(서경대) · 46(광운대) · 103(덕성여대) · 172(성신여대) · 343(한성대)
--   인기 부스 = 23·11·18 / 2217·2218·2219 / 4728·4729·4730 / 7736·7737·7738 / 13450·13451·13452
--   기존(적재 전) 주문 id ≤ 2828, 항목 id ≤ 4330, 단위 id ≤ 5310
--   대표 메뉴 = 서경대는 아래 목록 고정, 그 외 부스는 "품절 아님·0원 아님 메뉴 중 id가 가장 작은 2개"
--     (적재 후 메뉴를 품절로 바꾸면 이 SQL의 대표 메뉴 판정이 fixture와 달라질 수 있다 — 정확한 기준은 fixtures/menus.csv)
--
-- 500만 단계에서는 쿼리마다 수십 초 걸릴 수 있다.
-- =====================================================================================


-- [조건 0] 누적 주문 건수 — 기대: 단계 목표(50,000 / 100,000 / 500,000 / 1,000,000 / 5,000,000)
SELECT COUNT(*) AS orders FROM orders;


-- [조건 1] 학교 그룹 비중 — 기대: HOT 50% · WARM 35% · ETC 15%
--   (항목 없는 기존 주문 14건은 어느 그룹에도 속하지 않아 ETC가 약간 작게 나온다)
WITH ob AS (
  SELECT oi.order_id, MIN(bm.booth_id) AS booth_id
  FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
  GROUP BY oi.order_id
)
SELECT CASE WHEN d.university_id IN (1, 46, 103, 172, 343) THEN 'HOT'
            WHEN u.region = 'SEOUL' THEN 'WARM' ELSE 'ETC' END AS grp,
       COUNT(*) AS orders,
       ROUND(100 * COUNT(*) / (SELECT COUNT(*) FROM orders), 3) AS pct
FROM ob
JOIN booth b ON b.id = ob.booth_id
JOIN departments d ON d.id = b.department_id
JOIN universities u ON u.id = d.university_id
GROUP BY grp ORDER BY FIELD(grp, 'HOT', 'WARM', 'ETC');


-- [조건 2] HOT 학교별 비중과 인기 부스 3곳 합 — 기대: 학교마다 10%, 인기 3곳 합 70%(각 약 23.3%)
WITH ob AS (
  SELECT oi.order_id, MIN(bm.booth_id) AS booth_id
  FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
  GROUP BY oi.order_id
), per_booth AS (
  SELECT d.university_id, ob.booth_id, COUNT(*) AS orders
  FROM ob JOIN booth b ON b.id = ob.booth_id JOIN departments d ON d.id = b.department_id
  WHERE d.university_id IN (1, 46, 103, 172, 343)
  GROUP BY d.university_id, ob.booth_id
)
SELECT university_id,
       SUM(orders) AS school_orders,
       ROUND(100 * SUM(orders) / (SELECT COUNT(*) FROM orders), 3) AS school_pct,
       ROUND(100 * SUM(CASE WHEN booth_id IN (23, 11, 18, 2217, 2218, 2219, 4728, 4729, 4730,
                                              7736, 7737, 7738, 13450, 13451, 13452) THEN orders END)
             / SUM(orders), 3) AS popular3_pct
FROM per_booth GROUP BY university_id ORDER BY university_id;


-- [조건 2-1] HOT 인기 부스 각각의 비중 — 기대: 학교 주문의 약 23.3%씩
WITH ob AS (
  SELECT oi.order_id, MIN(bm.booth_id) AS booth_id
  FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
  GROUP BY oi.order_id
), per_booth AS (
  SELECT d.university_id, ob.booth_id, COUNT(*) AS orders,
         SUM(COUNT(*)) OVER (PARTITION BY d.university_id) AS school_orders
  FROM ob JOIN booth b ON b.id = ob.booth_id JOIN departments d ON d.id = b.department_id
  WHERE d.university_id IN (1, 46, 103, 172, 343)
  GROUP BY d.university_id, ob.booth_id
)
SELECT university_id, booth_id, orders, ROUND(100 * orders / school_orders, 2) AS pct_of_school
FROM per_booth
WHERE booth_id IN (23, 11, 18, 2217, 2218, 2219, 4728, 4729, 4730, 7736, 7737, 7738, 13450, 13451, 13452)
ORDER BY university_id, booth_id;


-- [조건 3] WARM·ETC 학교 간 균등 — 기대: 그룹마다 (최대 − 최소) ≤ 1
WITH ob AS (
  SELECT oi.order_id, MIN(bm.booth_id) AS booth_id
  FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
  GROUP BY oi.order_id
), per_school AS (
  SELECT d.university_id, CASE WHEN u.region = 'SEOUL' THEN 'WARM' ELSE 'ETC' END AS grp, COUNT(ob.order_id) AS orders
  FROM universities u
  JOIN departments d ON d.university_id = u.id
  JOIN booth b ON b.department_id = d.id
  LEFT JOIN ob ON ob.booth_id = b.id
  WHERE u.id NOT IN (1, 46, 103, 172, 343)
  GROUP BY d.university_id, grp
)
SELECT grp, COUNT(*) AS schools, MIN(orders) AS min_orders, MAX(orders) AS max_orders, SUM(orders) AS total
FROM per_school GROUP BY grp;


-- [조건 4] WARM·ETC 학교 안 부스 간 균등 — 기대: 학교마다 (최대 − 최소) ≤ 1 → 아래 결과가 0행
--   (주문 가능 메뉴가 3개 이상인 부스만 대상)
WITH ob AS (
  SELECT oi.order_id, MIN(bm.booth_id) AS booth_id
  FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
  GROUP BY oi.order_id
), eligible AS (
  SELECT booth_id FROM booth_menu WHERE is_sold_out = 0 AND price > 0 GROUP BY booth_id HAVING COUNT(*) >= 3
), per_booth AS (
  SELECT d.university_id, b.id AS booth_id, COUNT(ob.order_id) AS orders
  FROM booth b JOIN eligible e ON e.booth_id = b.id
  JOIN departments d ON d.id = b.department_id
  LEFT JOIN ob ON ob.booth_id = b.id
  WHERE d.university_id NOT IN (1, 46, 103, 172, 343)
  GROUP BY d.university_id, b.id
)
SELECT university_id, MIN(orders), MAX(orders)
FROM per_booth GROUP BY university_id HAVING MAX(orders) - MIN(orders) > 1;


-- [조건 5] 대표 메뉴 판매 수량 비중 — 기대: 65%
--   전체 합산 + HOT 인기 부스 15곳. 서경대 부스는 기존 주문(대표 비중 25~54%) 때문에 1단계에서는 낮다 (LLD-0006 14장 Q5)
WITH eligible_menu AS (
  SELECT id, booth_id, ROW_NUMBER() OVER (PARTITION BY booth_id ORDER BY id) AS rn
  FROM booth_menu WHERE is_sold_out = 0 AND price > 0
), seokyeong_rep (menu_id) AS (
  VALUES ROW(11),ROW(13),ROW(18),ROW(19),ROW(21),ROW(22),ROW(29),ROW(31),ROW(40),ROW(54),ROW(79),ROW(80),
         ROW(59),ROW(60),ROW(122),ROW(123),ROW(104),ROW(105),ROW(131),ROW(147),ROW(200),ROW(205),ROW(171),
         ROW(172),ROW(151),ROW(155),ROW(137),ROW(138),ROW(99),ROW(100),ROW(81),ROW(89),ROW(284),ROW(285),
         ROW(266),ROW(273),ROW(176),ROW(209),ROW(219),ROW(220),ROW(230),ROW(231),ROW(258),ROW(259),
         ROW(238),ROW(239),ROW(245),ROW(246),ROW(250),ROW(251),ROW(288),ROW(289)
), rep AS (
  SELECT em.id AS menu_id FROM eligible_menu em
  JOIN booth b ON b.id = em.booth_id JOIN departments d ON d.id = b.department_id
  WHERE d.university_id <> 1 AND em.rn <= 2
  UNION ALL SELECT menu_id FROM seokyeong_rep
), per_booth AS (
  SELECT bm.booth_id,
         SUM(CASE WHEN rep.menu_id IS NOT NULL THEN oi.quantity ELSE 0 END) AS rep_qty,
         SUM(oi.quantity) AS total_qty
  FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
  LEFT JOIN rep ON rep.menu_id = oi.booth_menu_id
  WHERE bm.booth_id NOT IN (1, 12, 35, 36, 38)   -- 신규 주문 대상에서 제외된 부스
  GROUP BY bm.booth_id
)
SELECT 'ALL' AS booth, SUM(rep_qty) AS rep_qty, SUM(total_qty) AS total_qty,
       ROUND(100 * SUM(rep_qty) / SUM(total_qty), 3) AS rep_pct FROM per_booth
UNION ALL
SELECT booth_id, rep_qty, total_qty, ROUND(100 * rep_qty / total_qty, 2) FROM per_booth
WHERE booth_id IN (23, 11, 18, 2217, 2218, 2219, 4728, 4729, 4730, 7736, 7737, 7738, 13450, 13451, 13452);


-- [조건 6] 주문 상태 비중 — 기대: WAITING 5% · COOKING 10% · COMPLETED 75% · CANCELED 10%
SELECT order_status, COUNT(*) AS orders, ROUND(100 * COUNT(*) / SUM(COUNT(*)) OVER (), 3) AS pct
FROM orders GROUP BY order_status ORDER BY FIELD(order_status, 'WAITING', 'COOKING', 'COMPLETED', 'CANCELED');


-- [조건 7] 주문당 메뉴 수 — 기대: 1개 10% · 2개 25% · 3개 50% · 4개 15%
--   (기존 주문 중 0개 14건, 5개 이상 12건이 섞여 있다)
SELECT k AS menus_per_order, COUNT(*) AS orders, ROUND(100 * COUNT(*) / SUM(COUNT(*)) OVER (), 3) AS pct
FROM (SELECT o.id, COUNT(oi.id) AS k FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id GROUP BY o.id) t
GROUP BY k ORDER BY k;


-- [조건 8] 항목당 수량 (신규 항목) — 기대: 1개 93.04% · 2개 5.68% · 3개 0.89% · 4개 0.32% · 5개 0.03% · 10개 0.03% 근처
SELECT quantity, COUNT(*) AS items, ROUND(100 * COUNT(*) / SUM(COUNT(*)) OVER (), 3) AS pct
FROM order_items WHERE id > 4330 GROUP BY quantity ORDER BY quantity;


-- [조건 9] 주문 시각 — 신규 주문의 시간대 분포. 기대: 18시 31.6% · 19시 25.7% · 20시 17.5% · 17시 7.8% … 근처
SELECT HOUR(created_at) AS hour, COUNT(*) AS orders, ROUND(100 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) AS pct
FROM orders WHERE id > 2828 GROUP BY hour ORDER BY hour;


-- [조건 9-1] 주문 시각 — 축제 일차별 비중. 기대: 1일차 20% · 2일차 42% · 3일차 38% 근처
--   (학교마다 축제 첫날이 다르므로 학교의 축제 첫날 기준으로 일차를 센다.
--    서경대는 부스에 테스트용 운영 일정이 섞여 있어 5/13으로 고정한다)
WITH ob AS (
  SELECT oi.order_id, MIN(bm.booth_id) AS booth_id
  FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
  WHERE oi.order_id > 2828 GROUP BY oi.order_id
), first_day AS (
  SELECT d.university_id,
         CASE WHEN d.university_id = 1 THEN DATE '2026-05-13' ELSE MIN(bo.operation_date) END AS d1
  FROM booth_operation bo JOIN booth b ON b.id = bo.booth_id JOIN departments d ON d.id = b.department_id
  WHERE bo.operation_date BETWEEN '2026-05-11' AND '2026-05-29'
  GROUP BY d.university_id
)
SELECT DATEDIFF(DATE(o.created_at), f.d1) + 1 AS festival_day, COUNT(*) AS orders,
       ROUND(100 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) AS pct
FROM orders o JOIN ob ON ob.order_id = o.id
JOIN booth b ON b.id = ob.booth_id JOIN departments d ON d.id = b.department_id
JOIN first_day f ON f.university_id = d.university_id
GROUP BY festival_day ORDER BY festival_day;


-- [조건 10] 정합성 — 기대: 전부 0
SELECT
  (SELECT COUNT(*) FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
    WHERE oi.id > 4330 AND (oi.menu_price <> bm.price OR oi.total_order_item_price <> oi.quantity * oi.menu_price))
    AS item_price_mismatch,
  (SELECT COUNT(*) FROM orders o JOIN (SELECT order_id, SUM(total_order_item_price) s FROM order_items GROUP BY order_id) t
    ON t.order_id = o.id WHERE o.id > 2828 AND o.total_order_price <> t.s)
    AS order_total_mismatch,
  (SELECT COUNT(*) FROM order_items oi LEFT JOIN (SELECT order_item_id, COUNT(*) c FROM order_item_units GROUP BY order_item_id) u
    ON u.order_item_id = oi.id WHERE oi.id > 4330 AND COALESCE(u.c, 0) <> oi.quantity)
    AS units_ne_quantity,
  (SELECT COUNT(*) FROM (SELECT oi.order_id FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
    GROUP BY oi.order_id HAVING COUNT(DISTINCT bm.booth_id) > 1) t)
    AS multi_booth_orders,
  (SELECT COUNT(*) FROM (SELECT order_id FROM order_items GROUP BY order_id, booth_menu_id HAVING COUNT(*) > 1) t)
    AS duplicate_menu_orders,
  (SELECT COUNT(*) FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
    WHERE oi.id > 4330 AND (bm.is_sold_out = 1 OR bm.price = 0))
    AS sold_out_or_free_menu_items,
  (SELECT COUNT(*) FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
    WHERE oi.id > 4330 AND bm.booth_id IN (1, 12, 35))
    AS excluded_booth_items;


-- [조건 11] 상태별 컬럼 규칙 (신규 주문) — 기대: 0
SELECT COUNT(*) AS status_column_violations FROM orders WHERE id > 2828 AND NOT (
  (order_status = 'WAITING' AND completed_at IS NULL AND canceled_at IS NULL AND order_cancel_reason IS NULL AND modified_at = created_at)
  OR (order_status = 'COOKING' AND completed_at IS NULL AND canceled_at IS NULL AND order_cancel_reason IS NULL AND modified_at > created_at)
  OR (order_status = 'COMPLETED' AND completed_at > created_at AND canceled_at IS NULL AND order_cancel_reason IS NULL AND modified_at = completed_at)
  OR (order_status = 'CANCELED' AND completed_at IS NULL AND canceled_at > created_at AND order_cancel_reason IS NOT NULL AND modified_at = canceled_at));


-- [조건 11-1] 상태별 서빙 여부 (신규 주문) — 기대: COMPLETED는 served_pct 100, WAITING·CANCELED는 0, COOKING은 그 사이
SELECT o.order_status, COUNT(*) AS units, ROUND(100 * AVG(u.is_served), 2) AS served_pct
FROM orders o JOIN order_items oi ON oi.order_id = o.id JOIN order_item_units u ON u.order_item_id = oi.id
WHERE o.id > 2828 GROUP BY o.order_status;


-- [조건 12] 기존 서경대 주문 보존 — 기대: 2,284건 / 합계 36,485,400 (fixtures/baseline.json)
SELECT COUNT(*) AS legacy_orders, SUM(total_order_price) AS legacy_total FROM orders WHERE id <= 2828;
