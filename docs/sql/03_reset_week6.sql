-- 6주차 Postman 컬렉션을 처음부터 다시 돌릴 때 쓰는 초기화 스크립트.
-- 시드(02_seed.sql)로 넣은 회원 2명·평점 3개만 남기고, 실습 중 만든 데이터를 지운다.
USE umc_movie;

DELETE FROM rating WHERE rating_id > 3;
DELETE FROM member WHERE member_id > 2;

-- 다음 회원가입이 memberId 3, 다음 평점이 ratingId 4부터 시작하도록 되돌린다.
ALTER TABLE member AUTO_INCREMENT = 3;
ALTER TABLE rating AUTO_INCREMENT = 4;
