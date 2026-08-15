USE erp_db;

-- 대분류
INSERT INTO category_main (name, display_order) VALUES
    ('타일/건축용접착제', 1),
    ('목공/지물용접착제', 2),
    ('타일/특수시멘트', 3),
    ('미장/도장/방수제', 4),
    ('건축용품', 5);

-- 중분류: 타일/건축용접착제
INSERT INTO category_sub (category_main_id, name, display_order)
SELECT id, sub.name, sub.ord FROM category_main,
    (SELECT '내장타일 접착제' AS name, 1 AS ord
     UNION ALL SELECT '고성능타일 접착제', 2
     UNION ALL SELECT '에폭시 접착제', 3
     UNION ALL SELECT 'PVC 바닥타일 접착제', 4
     UNION ALL SELECT '스티로폼 본드', 5) sub
WHERE category_main.name = '타일/건축용접착제';

-- 중분류: 목공/지물용접착제
INSERT INTO category_sub (category_main_id, name, display_order)
SELECT id, sub.name, sub.ord FROM category_main,
    (SELECT '목공용 접착제 705' AS name, 1 AS ord
     UNION ALL SELECT '목공용 접착제 701', 2
     UNION ALL SELECT '지물용 접착제', 3
     UNION ALL SELECT '인테리어필름 접착제', 4
     UNION ALL SELECT '중보행 접착제', 5) sub
WHERE category_main.name = '목공/지물용접착제';

-- 중분류: 타일/특수시멘트
INSERT INTO category_sub (category_main_id, name, display_order)
SELECT id, sub.name, sub.ord FROM category_main,
    (SELECT '압착용 타일 시멘트' AS name, 1 AS ord
     UNION ALL SELECT '내장 줄눈용 시멘트', 2
     UNION ALL SELECT '내장 칼라줄눈 시멘트', 3
     UNION ALL SELECT '외장 칼라줄눈 시멘트', 4
     UNION ALL SELECT '수지미장', 5
     UNION ALL SELECT '자동수평몰탈·특수몰탈', 6) sub
WHERE category_main.name = '타일/특수시멘트';

-- 중분류: 미장/도장/방수제
INSERT INTO category_sub (category_main_id, name, display_order)
SELECT id, sub.name, sub.ord FROM category_main,
    (SELECT '건축용 퍼티' AS name, 1 AS ord
     UNION ALL SELECT '몰탈접착강화제', 2
     UNION ALL SELECT '혼화제', 3
     UNION ALL SELECT '방수제/표면강화제', 4
     UNION ALL SELECT '발수제', 5
     UNION ALL SELECT '방동제', 6
     UNION ALL SELECT '프라이머', 7) sub
WHERE category_main.name = '미장/도장/방수제';

-- 중분류: 건축용품
INSERT INTO category_sub (category_main_id, name, display_order)
SELECT id, sub.name, sub.ord FROM category_main,
    (SELECT '실란트' AS name, 1 AS ord
     UNION ALL SELECT '우레탄폼', 2
     UNION ALL SELECT '코너비드', 3
     UNION ALL SELECT '유가', 4
     UNION ALL SELECT '스트레치필름', 5
     UNION ALL SELECT '타일클리너', 6
     UNION ALL SELECT '기타 건축용품', 7) sub
WHERE category_main.name = '건축용품';
