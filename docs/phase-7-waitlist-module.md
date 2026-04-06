# JoinUp Phase 7 Waitlist Module

## 1. `activity_waitlist` 表设计

```sql
ALTER TABLE `activity_waitlist`
    ADD COLUMN `confirmed_at` DATETIME(3) NULL COMMENT '确认转正时间' AFTER `promoted_at`,
    ADD COLUMN `confirm_deadline` DATETIME(3) NULL COMMENT '补位确认截止时间' AFTER `confirmed_at`,
    ADD COLUMN `promotion_source` TINYINT NULL COMMENT '补位来源:10正式取消触发 20人工触发 30超时顺延' AFTER `confirm_deadline`;

CREATE INDEX `idx_waitlist_status_deadline`
    ON `activity_waitlist` (`status`, `confirm_deadline`);
```

状态建议：
- `10 QUEUED`：排队中
- `20 WAITING_CONFIRM`：已发放补位，等待确认
- `30 PROMOTED`：已确认转正
- `40 CANCELED`：主动取消
- `50 EXPIRED`：补位超时失效

## 2. Redis 结构设计

- `joinup:waitlist:activity:{activityId}:queue`
  - 类型：`ZSET`
  - 作用：维护候补先后顺序
  - member：`userId`
  - score：`queueNo`
- `joinup:waitlist:activity:{activityId}:state`
  - 类型：`HASH`
  - 作用：维护活动级候补成员轻量状态
  - field：`userId`
  - value：`QUEUED / WAITING_CONFIRM`
- `joinup:waitlist:promotion:confirm-timeout`
  - 类型：`ZSET`
  - 作用：维护全局待超时确认索引
  - member：`activityId:userId`
  - score：`confirmDeadline` 毫秒时间戳
- `joinup:waitlist:activity:{activityId}:promotion:lock`
  - 类型：`Redisson Lock`
  - 作用：保证同一活动的补位推进动作串行执行

## 3. 候补入队逻辑

1. `joinup-signup` 在候补记录成功写库后发布 `WaitlistJoinedEvent`
2. `joinup-waitlist` 监听事件，并把用户写入 Redis ZSet 队列
3. 查询候补排名时优先读取 Redis 排名，缺失时回源数据库重建队列

## 4. 候补转正逻辑

1. 正式席位释放后，`joinup-signup` 发布 `FormalSignupCanceledEvent`
2. `joinup-waitlist` 在 Redisson 锁内选择队头有效候补用户
3. 候补记录状态改为 `WAITING_CONFIRM`
4. 写入通知表并发布 `WaitlistPromotionOfferedEvent`
5. `joinup-signup` 监听该事件，创建或更新正式报名记录并重新占用正式席位

## 5. 超时顺延逻辑

1. 候补模块定时扫描 `confirm-timeout` ZSet
2. 找到超时未确认记录后，将候补状态改为 `EXPIRED`
3. 发布 `WaitlistPromotionExpiredEvent`
4. `joinup-signup` 释放该用户占用的正式席位，并再次发布 `FormalSignupCanceledEvent`
5. 候补模块继续推进下一位

## 6. 对外接口

- `GET /api/waitlist/activity/{activityId}`：查询活动候补队列与当前用户排名
- `GET /api/waitlist/my`：查询当前用户的候补记录

## 7. 模块放置说明

- 候补主逻辑：`joinup-waitlist`
- 通知写库支撑：`joinup-notify`
- 候补事件模型：`joinup-common`
- 与报名模块的席位释放 / 补位占位衔接：`joinup-signup`

## 8. 并发控制建议

- 同一活动补位推进必须使用 `Redisson` 分布式锁
- 队头用户选择优先走 Redis ZSet，减少数据库排序压力
- 数据库层保留 `activity_id + user_id`、`activity_id + queue_no` 唯一约束兜底
- Redis 队列只作为高性能顺序索引，数据库仍是最终事实来源
