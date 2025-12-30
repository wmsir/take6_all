-- Cleanup old data
DELETE FROM game_history WHERE user_id = (SELECT id FROM Users WHERE wechat_openid = 'oTKf94hxsbzghhsbn12SJMvA3EcQ');
DELETE FROM Users WHERE wechat_openid = 'oTKf94hxsbzghhsbn12SJMvA3EcQ';

-- Insert Main User
INSERT INTO Users (wechat_openid, nickname, created_at, updated_at, vip_status) VALUES ('oTKf94hxsbzghhsbn12SJMvA3EcQ', '我 • Take6 玩家', '2025-12-04 12:11:49', '2025-12-16 12:11:49', 1);
SET @main_user_id = (SELECT id FROM Users WHERE wechat_openid = 'oTKf94hxsbzghhsbn12SJMvA3EcQ');

-- Insert Opponents if not exist
INSERT INTO Users (nickname, created_at, updated_at) SELECT '小王', '2025-12-04 12:11:49', '2025-12-16 12:11:49' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Users WHERE nickname = '小王');
SET @opp_id_0 = (SELECT id FROM Users WHERE nickname = '小王' LIMIT 1);
INSERT INTO Users (nickname, created_at, updated_at) SELECT '小李', '2025-12-04 12:11:49', '2025-12-16 12:11:49' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Users WHERE nickname = '小李');
SET @opp_id_1 = (SELECT id FROM Users WHERE nickname = '小李' LIMIT 1);
INSERT INTO Users (nickname, created_at, updated_at) SELECT '小陈', '2025-12-04 12:11:49', '2025-12-16 12:11:49' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Users WHERE nickname = '小陈');
SET @opp_id_2 = (SELECT id FROM Users WHERE nickname = '小陈' LIMIT 1);

-- Insert Game History (23 Games)
-- Game 1: L (Score 18) at 2025-12-05 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '54dd74ba', 18, 4, 13.50, '2025-12-05 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '54dd74ba', 12, 1, 13.50, '2025-12-05 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '54dd74ba', 13, 1, 13.50, '2025-12-05 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '54dd74ba', 11, 1, 13.50, '2025-12-05 00:11:49');

-- Game 2: L (Score 18) at 2025-12-05 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '33a0f719', 18, 4, 11.50, '2025-12-05 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '33a0f719', 11, 1, 11.50, '2025-12-05 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '33a0f719', 9, 1, 11.50, '2025-12-05 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '33a0f719', 8, 1, 11.50, '2025-12-05 12:11:49');

-- Game 3: W (Score 4) at 2025-12-06 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '4f7356df', 4, 1, 12.50, '2025-12-06 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '4f7356df', 17, 3, 12.50, '2025-12-06 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '4f7356df', 17, 3, 12.50, '2025-12-06 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '4f7356df', 12, 3, 12.50, '2025-12-06 00:11:49');

-- Game 4: W (Score 9) at 2025-12-06 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, 'f66c5da9', 9, 1, 18.25, '2025-12-06 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, 'f66c5da9', 20, 3, 18.25, '2025-12-06 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, 'f66c5da9', 23, 3, 18.25, '2025-12-06 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, 'f66c5da9', 21, 3, 18.25, '2025-12-06 12:11:49');

-- Game 5: W (Score 5) at 2025-12-07 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '0bdee89b', 5, 1, 12.50, '2025-12-07 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '0bdee89b', 12, 3, 12.50, '2025-12-07 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '0bdee89b', 14, 3, 12.50, '2025-12-07 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '0bdee89b', 19, 3, 12.50, '2025-12-07 00:11:49');

-- Game 6: W (Score 9) at 2025-12-07 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '60e37036', 9, 1, 16.25, '2025-12-07 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '60e37036', 20, 3, 16.25, '2025-12-07 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '60e37036', 21, 3, 16.25, '2025-12-07 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '60e37036', 15, 3, 16.25, '2025-12-07 12:11:49');

-- Game 7: L (Score 16) at 2025-12-08 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '36bb352e', 16, 4, 11.50, '2025-12-08 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '36bb352e', 10, 1, 11.50, '2025-12-08 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '36bb352e', 11, 1, 11.50, '2025-12-08 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '36bb352e', 9, 1, 11.50, '2025-12-08 00:11:49');

-- Game 8: L (Score 18) at 2025-12-08 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '767ffef9', 18, 4, 11.00, '2025-12-08 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '767ffef9', 8, 1, 11.00, '2025-12-08 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '767ffef9', 9, 1, 11.00, '2025-12-08 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '767ffef9', 9, 1, 11.00, '2025-12-08 12:11:49');

-- Game 9: W (Score 6) at 2025-12-09 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '550df3c9', 6, 1, 10.50, '2025-12-09 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '550df3c9', 11, 3, 10.50, '2025-12-09 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '550df3c9', 14, 3, 10.50, '2025-12-09 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '550df3c9', 11, 3, 10.50, '2025-12-09 00:11:49');

-- Game 10: W (Score 7) at 2025-12-09 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '33b2ec11', 7, 1, 16.75, '2025-12-09 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '33b2ec11', 21, 3, 16.75, '2025-12-09 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '33b2ec11', 17, 3, 16.75, '2025-12-09 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '33b2ec11', 22, 3, 16.75, '2025-12-09 12:11:49');

-- Game 11: L (Score 17) at 2025-12-10 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '2f75737a', 17, 4, 11.00, '2025-12-10 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '2f75737a', 8, 1, 11.00, '2025-12-10 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '2f75737a', 8, 1, 11.00, '2025-12-10 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '2f75737a', 11, 1, 11.00, '2025-12-10 00:11:49');

-- Game 12: L (Score 17) at 2025-12-10 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '5ef42fc2', 17, 4, 10.75, '2025-12-10 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '5ef42fc2', 10, 1, 10.75, '2025-12-10 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '5ef42fc2', 9, 1, 10.75, '2025-12-10 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '5ef42fc2', 7, 1, 10.75, '2025-12-10 12:11:49');

-- Game 13: D (Score 12) at 2025-12-11 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, 'a8e4448d', 12, 2, 14.00, '2025-12-11 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, 'a8e4448d', 10, 1, 14.00, '2025-12-11 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, 'a8e4448d', 17, 3, 14.00, '2025-12-11 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, 'a8e4448d', 17, 3, 14.00, '2025-12-11 00:11:49');

-- Game 14: W (Score 2) at 2025-12-11 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '91767b50', 2, 1, 9.25, '2025-12-11 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '91767b50', 12, 3, 9.25, '2025-12-11 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '91767b50', 12, 3, 9.25, '2025-12-11 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '91767b50', 11, 3, 9.25, '2025-12-11 12:11:49');

-- Game 15: W (Score 8) at 2025-12-12 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '0e24afdd', 8, 1, 12.75, '2025-12-12 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '0e24afdd', 13, 3, 12.75, '2025-12-12 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '0e24afdd', 16, 3, 12.75, '2025-12-12 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '0e24afdd', 14, 3, 12.75, '2025-12-12 00:11:49');

-- Game 16: D (Score 11) at 2025-12-12 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '02ba619b', 11, 2, 13.00, '2025-12-12 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '02ba619b', 9, 1, 13.00, '2025-12-12 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '02ba619b', 16, 3, 13.00, '2025-12-12 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '02ba619b', 16, 3, 13.00, '2025-12-12 12:11:49');

-- Game 17: L (Score 18) at 2025-12-13 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '90c81d10', 18, 4, 12.75, '2025-12-13 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '90c81d10', 10, 1, 12.75, '2025-12-13 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '90c81d10', 11, 1, 12.75, '2025-12-13 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '90c81d10', 12, 1, 12.75, '2025-12-13 00:11:49');

-- Game 18: W (Score 7) at 2025-12-13 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, '19c767b2', 7, 1, 13.00, '2025-12-13 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, '19c767b2', 13, 3, 13.00, '2025-12-13 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, '19c767b2', 17, 3, 13.00, '2025-12-13 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, '19c767b2', 15, 3, 13.00, '2025-12-13 12:11:49');

-- Game 19: L (Score 17) at 2025-12-14 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, 'e76727aa', 17, 4, 10.25, '2025-12-14 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, 'e76727aa', 9, 1, 10.25, '2025-12-14 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, 'e76727aa', 7, 1, 10.25, '2025-12-14 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, 'e76727aa', 8, 1, 10.25, '2025-12-14 00:11:49');

-- Game 20: D (Score 10) at 2025-12-14 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, 'ba1a68b4', 10, 2, 12.00, '2025-12-14 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, 'ba1a68b4', 8, 1, 12.00, '2025-12-14 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, 'ba1a68b4', 15, 3, 12.00, '2025-12-14 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, 'ba1a68b4', 15, 3, 12.00, '2025-12-14 12:11:49');

-- Game 21: W (Score 6) at 2025-12-15 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, 'd9ac8d87', 6, 1, 14.25, '2025-12-15 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, 'd9ac8d87', 15, 3, 14.25, '2025-12-15 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, 'd9ac8d87', 16, 3, 14.25, '2025-12-15 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, 'd9ac8d87', 20, 3, 14.25, '2025-12-15 00:11:49');

-- Game 22: W (Score 9) at 2025-12-15 12:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, 'aa482f6f', 9, 1, 17.25, '2025-12-15 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, 'aa482f6f', 23, 3, 17.25, '2025-12-15 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, 'aa482f6f', 21, 3, 17.25, '2025-12-15 12:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, 'aa482f6f', 16, 3, 17.25, '2025-12-15 12:11:49');

-- Game 23: L (Score 16) at 2025-12-16 00:11:49
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@main_user_id, 'dd892a0b', 16, 4, 10.75, '2025-12-16 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_0, 'dd892a0b', 8, 1, 10.75, '2025-12-16 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_1, 'dd892a0b', 10, 1, 10.75, '2025-12-16 00:11:49');
INSERT INTO game_history (user_id, room_id, score, `rank`, room_avg_score, created_at) VALUES (@opp_id_2, 'dd892a0b', 9, 1, 10.75, '2025-12-16 00:11:49');
