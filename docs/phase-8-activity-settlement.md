# JoinUp Phase 8 Activity Settlement Module

## 1. 成团 / 流局领域规则

1. 只允许处于 `SIGNUP_OPEN / FULL / WAITLIST_OPEN` 的活动参与截止结算。
2. 只有当 `signup_deadline <= 当前时间` 时，才允许触发结算。
3. 若 `current_participants >= min_group_size`，则状态变更为 `GROUP_SUCCESS`。
4. 若 `current_participants < min_group_size`，则状态变更为 `GROUP_FAILED`。
5. 一旦进入 `GROUP_SUCCESS / GROUP_FAILED`，活动不再处于开放报名状态，名单天然锁定。
6. 流局后通过事件通知下游模块关闭正式报名与候补记录；成团后通过事件通知下游关闭候补并锁定名单。

## 2. 定时任务逻辑

- 定时任务类：`ActivitySettlementScheduler`
- 定时入口：`scanAndSettleDueActivities()`
- 默认固定延迟：5 秒
- 扫描批次：默认 50 个活动

## 3. 状态流转

- `SIGNUP_OPEN -> GROUP_SUCCESS`
- `SIGNUP_OPEN -> GROUP_FAILED`
- `FULL -> GROUP_SUCCESS`
- `FULL -> GROUP_FAILED`
- `WAITLIST_OPEN -> GROUP_SUCCESS`
- `WAITLIST_OPEN -> GROUP_FAILED`

## 4. 状态日志写入

状态变更后统一调用 `ActivityStatusLogDomainService.recordStatusChange(...)`：
- 记录活动 ID
- 记录变更前后状态
- 记录系统判定原因
- 记录操作人（系统任务默认 0）
- 记录发生时间

## 5. 多实例避免重复执行方案

1. 先使用全局扫描锁：`joinup:activity:settlement:scan:lock`
2. 扫描到具体活动后，再使用活动级锁：`joinup:activity:settlement:activity:lock:{activityId}`
3. 这样既避免多节点重复扫描，也避免同一活动被重复结算

## 6. Kafka 事件发送设计

- Topic：`joinup.activity.settlement`
- 事件类型：`ActivitySettlementCompletedEvent`
- 发送时机：数据库事务提交成功后，通过 `@TransactionalEventListener(AFTER_COMMIT)` 发往 Kafka
- 事件关键字段：
  - `activityId`
  - `organizerId`
  - `fromStatus`
  - `toStatus`
  - `currentParticipants`
  - `minGroupSize`
  - `rosterLocked`
  - `closeSignups`
  - `closeWaitlists`
  - `reason`

## 7. 后续状态处理说明

- 成团成功：
  - 活动名单锁定
  - 下游可根据事件关闭候补、发送成团通知
- 成团失败：
  - 活动流局
  - 下游可根据事件结束正式报名与候补记录，并发送流局通知

## 8. 模块放置建议

- 结算规则 / 锁 / 日志 / 调度 / 事件发送：`joinup-activity`
- 事件模型：`joinup-common`
- 通知与报名、候补的后续收口动作：后续由对应模块消费 Kafka 事件处理