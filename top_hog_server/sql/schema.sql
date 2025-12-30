-- 创建数据库
CREATE DATABASE IF NOT EXISTS `top_hog_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `top_hog_db`;

-- 用户表
-- 存储用户的基本信息、账户凭证和微信/QQ绑定信息
CREATE TABLE IF NOT EXISTS `Users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名 (微信登录时可选，随机生成)',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电子邮箱 (微信登录时可选)',
  `password` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码 (微信登录时可选)',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像URL',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号码',
  `wechat_openid` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信 OpenID',
  `wechat_session_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信session_key',
  `qq_openid` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'QQ OpenID',
  `invite_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邀请码',
  `vip_status` int(11) DEFAULT '0' COMMENT 'VIP状态 (0: 非会员, >0: 会员等级)',
  `email_verified` tinyint(1) DEFAULT '0' COMMENT '邮箱是否已验证 (0: 否, 1: 是)',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  UNIQUE KEY `uk_wechat_openid` (`wechat_openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 游戏房间表
-- 存储游戏房间的配置和当前状态
CREATE TABLE IF NOT EXISTS `game_room` (
  `room_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '房间ID (UUID或短码)',
  `room_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房间名称',
  `game_state` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'WAITING' COMMENT '游戏状态 (WAITING, PLAYING, GAME_OVER等)',
  `max_players` int(11) NOT NULL DEFAULT '6' COMMENT '最大玩家数',
  `current_round` int(11) NOT NULL DEFAULT '1' COMMENT '当前轮数',
  `max_rounds` int(11) NOT NULL DEFAULT '3' COMMENT '最大轮数',
  `target_score` int(11) NOT NULL DEFAULT '66' COMMENT '目标分数 (触发结束的分数)',
  `is_private` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否私密房间',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房间密码 (私密房间用)',
  `owner_id` bigint(20) DEFAULT NULL COMMENT '房主ID (关联 Users.id)',
  PRIMARY KEY (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏房间表';

-- 游戏历史记录表
-- 存储玩家的对局战绩
CREATE TABLE IF NOT EXISTS `game_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID，主键',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID (关联 Users.id)',
  `room_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房间ID',
  `score` int(11) NOT NULL COMMENT '本局得分 (猪头数)',
  `rank` int(11) NOT NULL COMMENT '本局排名',
  `room_avg_score` double DEFAULT NULL COMMENT '本局房间平均得分',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏战绩历史表';
