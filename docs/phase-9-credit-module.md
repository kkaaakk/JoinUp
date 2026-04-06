# 第9段：用户信用模块

## 1. 信用规则设计

当前信用规则采用“用户表保存当前分数、信用记录表保存变化明细”的方式落地：

1. 正常参加活动：`+1`
2. 允许时间内主动取消：默认 `0`
3. 活动开始前临时取消：`-3`
4. 爽约未到场：`-10`
5. 管理员人工调整：按请求值增减

规则都做成了配置项，放在 `joinup.credit.*` 下，后续运营调整不用改代码。

## 2. 限制规则设计

当前限制采用“分数阈值 + 连续爽约”双条件组合：

1. 信用分低于 `create-activity-min-score`：限制创建活动
2. 信用分低于 `hot-activity-signup-min-score`：限制报名热门活动
3. 信用分低于 `waitlist-penalty-min-score`：降低候补优先级
4. 最近行为中连续爽约达到 `consecutive-no-show-limit`：直接触发热门活动报名限制与候补优先级下降

热门活动由 `activity.heat_score >= hot-activity-heat-score-threshold` 判定。

## 3. 代码落位

### 模块

- `joinup-credit`：信用模块主体
- `joinup-common`：新增信用错误码
- `joinup-boot`：新增信用规则配置

### 关键文件

- `joinup-credit/src/main/java/com/joinup/credit/service/CreditService.java`
- `joinup-credit/src/main/java/com/joinup/credit/service/impl/CreditServiceImpl.java`
- `joinup-credit/src/main/java/com/joinup/credit/domain/CreditRuleEngine.java`
- `joinup-credit/src/main/java/com/joinup/credit/domain/CreditRestrictionChecker.java`
- `joinup-credit/src/main/java/com/joinup/credit/controller/CreditController.java`
- `joinup-credit/src/main/java/com/joinup/credit/controller/AdminCreditController.java`

## 4. 表结构对应代码

`user_credit_record` 表对应：

- `UserCreditRecordEntity`
- `UserCreditRecordMapper`
- `CreditChangeTypeEnum`

说明：

1. `before_score / after_score` 保留快照，便于后续审计和纠纷排查
2. `related_activity_id / related_signup_id` 作为业务链路锚点，方便串联活动和报名记录
3. 当前信用分冗余保存在 `user.credit_score`，便于高频读取，不必每次聚合明细

## 5. 与报名模块、活动模块的衔接点

当前已经在信用服务里提供了直接可调用的接口：

1. `assertCanSignup(CreditSignupCheckRequest)`
   供 `joinup-signup` 在 Lua 抢位前做前置准入校验

2. `shouldDeprioritizeWaitlist(Long userId)`
   供 `joinup-waitlist` 在候补排序策略里做优先级折损

3. `recordSignupCancel(CreditCancelRequest)`
   供 `joinup-signup / orchestration` 在取消报名后写入信用变化

4. `recordNoShowPenalty(CreditNoShowPenaltyRequest)`
   供签到、履约判定或活动完结编排模块在识别爽约后调用

5. `recordAttendanceReward(CreditAttendanceRewardRequest)`
   供活动完结、签到完成等场景发放守约加分

6. `assertCanCreateActivity(Long userId)`
   供 `joinup-activity` 在创建活动前做信用准入

说明：

这一段没有擅自去改报名模块和活动模块的主流程，而是先把可直接接入的服务契约补齐，保持模块边界清晰。

## 6. 对外接口

### 用户侧

- `GET /api/credit/my`

返回内容包含：

1. 当前信用分
2. 连续爽约次数
3. 是否允许创建活动
4. 是否允许报名热门活动
5. 是否需要降低候补优先级
6. 最近信用变更记录

### 管理侧

- `POST /api/admin/credit/adjust/{userId}`

当前管理权限仍沿用临时规则：用户名为 `admin` 视为管理员。后续接入 RBAC 时，只需要替换 `CreditPermissionChecker`。