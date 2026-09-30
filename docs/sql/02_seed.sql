-- 더미 데이터. password는 실제 비밀번호가 아니라 bcrypt 모양의 더미 문자열이다. (6주차에 bcrypt 적용)
USE umc_movie;

INSERT INTO member (email, password, nickname) VALUES
('noco@umc.com', '$2a$10$dummyEncodedPasswordValue', '수현'),
('minseo@umc.com', '$2a$10$dummyEncodedPasswordValue', '민서');

-- movie_id는 TMDB의 실제 영화 id (550 파이트 클럽, 155 다크 나이트)
INSERT INTO rating (member_id, movie_id, score, comment) VALUES
(1, 550, 5, '최고의 영화!'),
(1, 155, 4, '다시 봐도 명작'),
(2, 550, 3, '호불호가 갈릴 것 같아요');
