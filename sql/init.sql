-- ============================================
-- 跑步数据分析平台 - 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS `run_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `run_ai`;

-- 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    `gender` TINYINT DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `height_cm` DOUBLE DEFAULT NULL COMMENT '身高(cm)',
    `weight_kg` DOUBLE DEFAULT NULL COMMENT '体重(kg)',
    `max_heart_rate` INT DEFAULT NULL COMMENT '最大心率',
    `rest_heart_rate` INT DEFAULT NULL COMMENT '静息心率',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 第三方平台授权表
CREATE TABLE IF NOT EXISTS `t_user_platform_auth` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `platform` VARCHAR(20) NOT NULL COMMENT '平台: COROS/GARMIN/STRAVA',
    `platform_user_id` VARCHAR(100) NOT NULL COMMENT '平台用户ID',
    `access_token` VARCHAR(500) NOT NULL COMMENT '访问令牌',
    `refresh_token` VARCHAR(500) DEFAULT NULL COMMENT '刷新令牌',
    `token_expire_time` DATETIME DEFAULT NULL COMMENT '令牌过期时间',
    `sync_status` TINYINT DEFAULT 0 COMMENT '同步状态 0-未同步 1-同步中 2-已完成 3-失败',
    `last_sync_time` DATETIME DEFAULT NULL COMMENT '最后同步时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_platform` (`user_id`, `platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方平台授权表';

-- 跑步活动表
CREATE TABLE IF NOT EXISTS `t_running_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `platform` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源',
    `platform_activity_id` VARCHAR(100) DEFAULT NULL COMMENT '平台活动ID',
    `activity_name` VARCHAR(200) DEFAULT NULL COMMENT '活动名称',
    `activity_type` VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '活动类型',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `duration_seconds` INT NOT NULL COMMENT '运动时长(秒)',
    `distance_m` DOUBLE NOT NULL COMMENT '距离(米)',
    `avg_pace_sec_km` INT DEFAULT NULL COMMENT '平均配速(秒/公里)',
    `max_pace_sec_km` INT DEFAULT NULL COMMENT '最快配速',
    `avg_heart_rate` INT DEFAULT NULL COMMENT '平均心率',
    `max_heart_rate` INT DEFAULT NULL COMMENT '最大心率',
    `avg_cadence` INT DEFAULT NULL COMMENT '平均步频',
    `avg_stride_m` DOUBLE DEFAULT NULL COMMENT '平均步幅(米)',
    `calories` INT DEFAULT NULL COMMENT '卡路里(千卡)',
    `elevation_gain_m` DOUBLE DEFAULT NULL COMMENT '累计爬升(米)',
    `elevation_loss_m` DOUBLE DEFAULT NULL COMMENT '累计下降(米)',
    `gpx_url` VARCHAR(500) DEFAULT NULL COMMENT 'GPX文件地址',
    `map_image_url` VARCHAR(500) DEFAULT NULL COMMENT '轨迹图地址',
    `training_effect_aerobic` DOUBLE DEFAULT NULL COMMENT '有氧训练效果',
    `training_effect_anaerobic` DOUBLE DEFAULT NULL COMMENT '无氧训练效果',
    `vo2max` DOUBLE DEFAULT NULL COMMENT 'VO2Max',
    `remark` TEXT DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_activity` (`platform`, `platform_activity_id`),
    KEY `idx_user_time` (`user_id`, `start_time`),
    KEY `idx_user_type` (`user_id`, `activity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跑步活动表';

-- 跑步分段数据表
CREATE TABLE IF NOT EXISTS `t_running_lap` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `lap_index` INT NOT NULL COMMENT '分段序号',
    `split_distance_m` DOUBLE NOT NULL COMMENT '分段距离(米)',
    `split_duration_sec` INT NOT NULL COMMENT '分段用时(秒)',
    `avg_pace_sec_km` INT DEFAULT NULL COMMENT '分段配速',
    `avg_heart_rate` INT DEFAULT NULL COMMENT '分段平均心率',
    `max_heart_rate` INT DEFAULT NULL COMMENT '分段最大心率',
    `avg_cadence` INT DEFAULT NULL COMMENT '分段步频',
    `elevation_gain_m` DOUBLE DEFAULT NULL COMMENT '分段爬升',
    PRIMARY KEY (`id`),
    KEY `idx_activity` (`activity_id`, `lap_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跑步分段数据表';

-- 跑步轨迹点表
CREATE TABLE IF NOT EXISTS `t_running_track_point` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `point_index` INT NOT NULL COMMENT '点序号',
    `latitude` DOUBLE NOT NULL COMMENT '纬度',
    `longitude` DOUBLE NOT NULL COMMENT '经度',
    `elevation_m` DOUBLE DEFAULT NULL COMMENT '海拔',
    `heart_rate` INT DEFAULT NULL COMMENT '心率',
    `cadence` INT DEFAULT NULL COMMENT '步频',
    `speed_ms` DOUBLE DEFAULT NULL COMMENT '瞬时速度',
    `timestamp` DATETIME NOT NULL COMMENT '时间戳',
    PRIMARY KEY (`id`),
    KEY `idx_activity_index` (`activity_id`, `point_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跑步轨迹点表';

-- 跑步分析结果表
CREATE TABLE IF NOT EXISTS `t_running_analysis` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `activity_id` BIGINT NOT NULL COMMENT '关联活动ID',
    `analysis_type` VARCHAR(30) NOT NULL COMMENT '分析类型',
    `analysis_data` JSON NOT NULL COMMENT '分析结果JSON',
    `ai_summary` TEXT DEFAULT NULL COMMENT 'AI分析摘要',
    `score` INT DEFAULT NULL COMMENT '跑步评分(0-100)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_activity` (`user_id`, `activity_id`),
    KEY `idx_user_type` (`user_id`, `analysis_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跑步分析结果表';

-- 训练计划表
CREATE TABLE IF NOT EXISTS `t_training_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `plan_name` VARCHAR(100) NOT NULL COMMENT '计划名称',
    `goal_type` VARCHAR(30) NOT NULL COMMENT '目标类型',
    `goal_value` VARCHAR(50) DEFAULT NULL COMMENT '目标值',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `weekly_plan` JSON NOT NULL COMMENT '周计划',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未开始 1-进行中 2-已完成 3-已终止',
    `ai_reasoning` TEXT DEFAULT NULL COMMENT 'AI推理说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练计划表';

-- 训练计划每日详情表
CREATE TABLE IF NOT EXISTS `t_training_plan_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `plan_id` BIGINT NOT NULL COMMENT '计划ID',
    `plan_date` DATE NOT NULL COMMENT '日期',
    `day_of_week` TINYINT NOT NULL COMMENT '星期几 1-7',
    `workout_type` VARCHAR(30) NOT NULL COMMENT '训练类型',
    `target_distance_km` DOUBLE DEFAULT NULL COMMENT '目标距离(km)',
    `target_duration_min` INT DEFAULT NULL COMMENT '目标时长(分钟)',
    `target_pace_sec_km` INT DEFAULT NULL COMMENT '目标配速',
    `target_hr_zone` VARCHAR(10) DEFAULT NULL COMMENT '目标心率区间',
    `workout_description` TEXT NOT NULL COMMENT '训练内容描述',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待完成 1-已完成 2-跳过',
    `actual_activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_plan_date` (`plan_id`, `plan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练计划每日详情表';

-- 用户运动目标表
CREATE TABLE IF NOT EXISTS `t_user_goal` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `goal_type` VARCHAR(30) NOT NULL COMMENT '目标类型',
    `target_value` DOUBLE NOT NULL COMMENT '目标值',
    `current_value` DOUBLE NOT NULL DEFAULT 0 COMMENT '当前进度',
    `unit` VARCHAR(10) NOT NULL COMMENT '单位',
    `deadline` DATE DEFAULT NULL COMMENT '截止日期',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-进行中 1-已完成 2-已过期',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户运动目标表';
