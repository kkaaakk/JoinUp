-- JoinUp Phase 7 Waitlist Upgrade

ALTER TABLE `activity_waitlist`
    ADD COLUMN IF NOT EXISTS `confirmed_at` DATETIME(3) NULL COMMENT '确认转正时间' AFTER `promoted_at`,
    ADD COLUMN IF NOT EXISTS `confirm_deadline` DATETIME(3) NULL COMMENT '补位确认截止时间' AFTER `confirmed_at`,
    ADD COLUMN IF NOT EXISTS `promotion_source` TINYINT NULL COMMENT '补位来源:10正式取消触发 20人工触发 30超时顺延' AFTER `confirm_deadline`;

CREATE INDEX `idx_waitlist_status_deadline`
    ON `activity_waitlist` (`status`, `confirm_deadline`);
