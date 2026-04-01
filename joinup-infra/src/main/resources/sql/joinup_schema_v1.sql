-- JoinUp Phase 3 Database Schema
-- MySQL 8.x

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态:1正常 2禁用 3注销',
    `credit_score` INT NOT NULL DEFAULT 100 COMMENT '信用分',
    `last_login_at` DATETIME(3) DEFAULT NULL COMMENT '最后登录时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    UNIQUE KEY `uk_user_email` (`email`),
    KEY `idx_user_status_credit` (`status`, `credit_score`),
    KEY `idx_user_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `user_profile` (
    `id` BIGINT NOT NULL COMMENT '资料ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '性别:0未知 1男 2女 3其他',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `school_name` VARCHAR(128) DEFAULT NULL COMMENT '学校',
    `major` VARCHAR(128) DEFAULT NULL COMMENT '专业',
    `bio` VARCHAR(500) DEFAULT NULL COMMENT '个性签名',
    `city` VARCHAR(64) DEFAULT NULL COMMENT '城市',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_profile_user_id` (`user_id`),
    KEY `idx_profile_nickname` (`nickname`),
    KEY `idx_profile_school` (`school_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资料表';

CREATE TABLE IF NOT EXISTS `activity` (
    `id` BIGINT NOT NULL COMMENT '活动ID',
    `organizer_id` BIGINT NOT NULL COMMENT '发起人ID',
    `title` VARCHAR(128) NOT NULL COMMENT '活动标题',
    `description` VARCHAR(2000) DEFAULT NULL COMMENT '活动描述',
    `category` VARCHAR(32) NOT NULL COMMENT '活动类别',
    `location_text` VARCHAR(255) NOT NULL COMMENT '活动地点',
    `start_time` DATETIME(3) NOT NULL COMMENT '活动开始时间',
    `end_time` DATETIME(3) NOT NULL COMMENT '活动结束时间',
    `signup_deadline` DATETIME(3) NOT NULL COMMENT '报名截止时间',
    `max_participants` INT NOT NULL COMMENT '最大人数',
    `min_group_size` INT NOT NULL COMMENT '最少成团人数',
    `current_participants` INT NOT NULL DEFAULT 0 COMMENT '当前确认人数',
    `waitlist_count` INT NOT NULL DEFAULT 0 COMMENT '候补人数',
    `allow_waitlist` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许候补:0否 1是',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '活动状态:10草稿 20报名中 30已满 40成团 50流局 60已取消 70已结束',
    `heat_score` INT NOT NULL DEFAULT 0 COMMENT '活动热度',
    `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    KEY `idx_activity_organizer` (`organizer_id`),
    KEY `idx_activity_status_deadline` (`status`, `signup_deadline`),
    KEY `idx_activity_start_time` (`start_time`),
    KEY `idx_activity_heat` (`heat_score`),
    KEY `idx_activity_category_status` (`category`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动表';

CREATE TABLE IF NOT EXISTS `activity_tag` (
    `id` BIGINT NOT NULL COMMENT '标签ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `tag_name` VARCHAR(32) NOT NULL COMMENT '标签名称',
    `tag_type` TINYINT NOT NULL DEFAULT 1 COMMENT '标签类型:1系统 2自定义',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_tag` (`activity_id`, `tag_name`),
    KEY `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动标签表';

CREATE TABLE IF NOT EXISTS `activity_signup` (
    `id` BIGINT NOT NULL COMMENT '报名ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '报名状态:10已报名 20已取消 30已爽约 40已完成 50候补转正',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '报名来源:1直接报名 2候补补位',
    `signup_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '报名时间',
    `cancel_time` DATETIME(3) DEFAULT NULL COMMENT '取消时间',
    `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_signup_activity_user` (`activity_id`, `user_id`),
    KEY `idx_signup_user_status` (`user_id`, `status`),
    KEY `idx_signup_activity_status` (`activity_id`, `status`),
    KEY `idx_signup_time` (`signup_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报名表';

CREATE TABLE IF NOT EXISTS `activity_waitlist` (
    `id` BIGINT NOT NULL COMMENT '候补ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `queue_no` INT NOT NULL COMMENT '候补顺位(1开始)',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '候补状态:10排队中 20已补位 30已取消 40已失效',
    `joined_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入队时间',
    `promoted_at` DATETIME(3) DEFAULT NULL COMMENT '补位时间',
    `expired_at` DATETIME(3) DEFAULT NULL COMMENT '失效时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_waitlist_activity_user` (`activity_id`, `user_id`),
    UNIQUE KEY `uk_waitlist_activity_queue` (`activity_id`, `queue_no`),
    KEY `idx_waitlist_activity_status_queue` (`activity_id`, `status`, `queue_no`),
    KEY `idx_waitlist_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='候补表';

CREATE TABLE IF NOT EXISTS `activity_status_log` (
    `id` BIGINT NOT NULL COMMENT '日志ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `from_status` TINYINT NOT NULL COMMENT '变更前状态',
    `to_status` TINYINT NOT NULL COMMENT '变更后状态',
    `reason` VARCHAR(255) DEFAULT NULL COMMENT '状态变更原因',
    `operator_id` BIGINT NOT NULL DEFAULT 0 COMMENT '操作人ID',
    `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '变更时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_activity_status_log_activity_time` (`activity_id`, `occurred_at`),
    KEY `idx_activity_status_log_to_status` (`to_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动状态日志表';

CREATE TABLE IF NOT EXISTS `user_credit_record` (
    `id` BIGINT NOT NULL COMMENT '信用记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `change_type` TINYINT NOT NULL COMMENT '变更类型:10报名奖励 20守约奖励 30取消扣分 40爽约扣分 50运营调整',
    `delta_score` INT NOT NULL COMMENT '变更分值(可正可负)',
    `before_score` INT NOT NULL COMMENT '变更前分数',
    `after_score` INT NOT NULL COMMENT '变更后分数',
    `reason` VARCHAR(255) NOT NULL COMMENT '变更原因',
    `related_activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID',
    `related_signup_id` BIGINT DEFAULT NULL COMMENT '关联报名ID',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_credit_user_created` (`user_id`, `created_at`),
    KEY `idx_credit_change_type` (`change_type`),
    KEY `idx_credit_related_activity` (`related_activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信用记录表';

CREATE TABLE IF NOT EXISTS `notify_message` (
    `id` BIGINT NOT NULL COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `message_type` TINYINT NOT NULL COMMENT '消息类型:10活动提醒 20报名结果 30候补补位 40信用变更 50系统通知',
    `channel` TINYINT NOT NULL DEFAULT 10 COMMENT '渠道:10站内信 20短信 30邮件',
    `title` VARCHAR(128) NOT NULL COMMENT '标题',
    `content` VARCHAR(2000) NOT NULL COMMENT '内容',
    `biz_type` VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    `biz_id` BIGINT DEFAULT NULL COMMENT '业务ID',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '通知状态:10待发送 20发送成功 30发送失败 40已读',
    `send_time` DATETIME(3) DEFAULT NULL COMMENT '发送时间',
    `read_time` DATETIME(3) DEFAULT NULL COMMENT '阅读时间',
    `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_notify_user_status` (`user_id`, `status`),
    KEY `idx_notify_status_send_time` (`status`, `send_time`),
    KEY `idx_notify_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知消息表';

CREATE TABLE IF NOT EXISTS `activity_report` (
    `id` BIGINT NOT NULL COMMENT '举报ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `reporter_user_id` BIGINT NOT NULL COMMENT '举报人ID',
    `reported_user_id` BIGINT DEFAULT NULL COMMENT '被举报用户ID',
    `report_type` TINYINT NOT NULL COMMENT '举报类型:10违规内容 20骚扰 30爽约 40其他',
    `reason` VARCHAR(500) NOT NULL COMMENT '举报原因',
    `evidence_urls` VARCHAR(2000) DEFAULT NULL COMMENT '证据URL(JSON数组或逗号分隔)',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '处理状态:10待处理 20处理中 30已通过 40已驳回',
    `handler_user_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
    `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    `handled_at` DATETIME(3) DEFAULT NULL COMMENT '处理时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_report_activity_status` (`activity_id`, `status`),
    KEY `idx_report_reporter` (`reporter_user_id`),
    KEY `idx_report_reported_user` (`reported_user_id`),
    KEY `idx_report_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动举报表';

CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL COMMENT '日志ID',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_role` VARCHAR(32) DEFAULT NULL COMMENT '操作角色',
    `module_name` VARCHAR(64) NOT NULL COMMENT '模块名',
    `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型',
    `biz_type` VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    `biz_id` BIGINT DEFAULT NULL COMMENT '业务ID',
    `request_id` VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    `request_path` VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
    `request_method` VARCHAR(16) DEFAULT NULL COMMENT 'HTTP方法',
    `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT 'UserAgent',
    `operation_result` TINYINT NOT NULL DEFAULT 10 COMMENT '操作结果:10成功 20失败',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展字段',
    `operation_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updated_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_operation_module_time` (`module_name`, `operation_time`),
    KEY `idx_operation_operator_time` (`operator_id`, `operation_time`),
    KEY `idx_operation_result_time` (`operation_result`, `operation_time`),
    KEY `idx_operation_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
