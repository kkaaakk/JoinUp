# JoinUp Backend

## 项目说明
JoinUp（组个局）是面向校园和社区的线下临时活动组局平台后端。
本仓库采用模块化单体架构，当前阶段优先建设可启动、可扩展的基础骨架。

## 分段交付记录

### 第1段（已完成）
完成时间：2026-04-01

1. 项目整体架构设计
2. Maven 多模块划分方案
3. 模块职责说明
4. 技术选型说明
5. 推荐 package 结构
6. 后续生成代码的分步计划

### 第2段（已完成）
完成时间：2026-04-01

本段目标：先生成项目骨架与基础设施层，不扩展业务细节。

已完成项：
1. Maven 多模块结构（新命名）
- `joinup-boot`
- `joinup-common`
- `joinup-infra`
- `joinup-user`
- `joinup-activity`
- `joinup-signup`
- `joinup-waitlist`
- `joinup-credit`
- `joinup-notify`
- `joinup-admin`

2. 父工程与各子模块 `pom.xml`
3. 启动类与 `application.yml` 模板
4. MySQL / Redis / Kafka 配置骨架
5. Spring Security + JWT 认证骨架
6. 全局异常处理
7. 统一返回结构 `Result<T>`
8. 基础枚举与常量骨架
9. Swagger / Knife4j 配置
10. MyBatis Plus 配置
11. 通用审计字段基类（`created_at`、`updated_at`、`deleted` 等）
12. 新增模块分层目录骨架（`controller/service/domain/mapper/entity/dto/vo`）

补充说明：
- 旧目录（`joinup-app`、`joinup-infrastructure`、`joinup-module-*`）已删除。
- 目前 parent 聚合只包含新模块命名。

## 当前模块清单（生效）
- joinup-common
- joinup-infra
- joinup-user
- joinup-activity
- joinup-signup
- joinup-waitlist
- joinup-credit
- joinup-notify
- joinup-admin
- joinup-boot

## 后续约定
从现在开始，每完成一段都会同步更新本 README 的“分段交付记录”，至少包含：
1. 段号
2. 完成时间
3. 本段目标
4. 已完成内容
5. 影响模块
6. 备注（如迁移、兼容、已知限制）

### 第3段（已完成）
完成时间：2026-04-01

本段目标：生成数据库设计与实体骨架，先不扩展业务实现。

已完成项：
1. 11 张核心业务表 MySQL DDL（含关键约束与索引）
2. 每张表字段说明与索引设计建议文档
3. MyBatis Plus Entity 骨架（按模块落位）
4. 枚举字段设计与对应 Enum 骨架
5. 统一逻辑删除与审计字段方案说明

关键约束已覆盖：
- `activity_signup(activity_id, user_id)` 唯一索引
- `activity_waitlist(activity_id, user_id)` 唯一索引
- `activity_waitlist(activity_id, queue_no)` 唯一索引
- `user_profile(user_id)` 唯一索引

影响模块：
- joinup-infra（SQL）
- joinup-user / joinup-activity / joinup-signup / joinup-waitlist / joinup-credit / joinup-notify / joinup-admin（Entity + Enum）

相关文件：
- `joinup-infra/src/main/resources/sql/joinup_schema_v1.sql`
- `docs/phase-3-database-design.md`

### 第4段（已完成）
完成时间：2026-04-02

本段目标：实现 `joinup-user` 模块的注册、登录、JWT 鉴权、个人资料与信用分接口。

已完成项：
1. `joinup-user` 的 DTO / VO / Mapper / Service / Controller
2. 用户注册、登录、当前资料查询、资料修改、信用分查询
3. 基于 JWT 的登录认证链路接入
4. Spring Security 放行路径调整到 `/api/user/register` 与 `/api/user/login`
5. 密码加密存储与用户状态校验

影响模块：
- `joinup-user`
- `joinup-infra`
- `joinup-common`

相关文件：
- `docs/phase-4-user-module.md`
- `joinup-user/src/main/java/com/joinup/user/controller/UserController.java`
- `joinup-user/src/main/java/com/joinup/user/service/impl/UserServiceImpl.java`

### 第5段（已完成）
完成时间：2026-04-02

本段目标：实现 `joinup-activity` 模块的活动创建、修改、详情、分页、取消与管理员审核骨架。

已完成项：
1. `activity / activity_tag` 相关 DTO / VO / Entity / Mapper / Service / Controller
2. 活动状态枚举与集中式状态流转设计
3. 发起者权限校验与管理员审核占位校验
4. 活动详情、分页、取消、审核接口骨架
5. `waitlist_limit / view_count / reviewed_*` 字段预留接入

影响模块：
- `joinup-activity`
- `joinup-common`
- `joinup-infra`

相关文件：
- `docs/phase-5-activity-module.md`
- `joinup-activity/src/main/java/com/joinup/activity/service/impl/ActivityServiceImpl.java`
- `joinup-activity/src/main/java/com/joinup/activity/domain/ActivityStatusFlow.java`
补充约定：后续每一段业务交付默认同步补充必要代码注释，但补注释本身不单独占用新的段号。
### 第6段（已完成）
完成时间：2026-04-02

本段目标：实现 `joinup-signup` 高并发报名抢位模块，覆盖 Redis 名额缓存、Lua 原子扣减、幂等控制、Kafka 异步写库、报名结果查询与必要补偿。
已完成项：
1. `POST /api/signup/apply/{activityId}` 报名接口，正式名额不足时自动切候补逻辑
2. `POST /api/signup/cancel/{activityId}` 取消接口骨架，异步取消并回补 Redis 名额
3. `GET /api/signup/my` 与 `GET /api/signup/activity/{activityId}` 结果查询接口
4. Redis Key 设计与活动快照缓存初始化
5. `signup_apply.lua` 原子报名脚本与 `signup_compensate.lua` 补偿脚本
6. Kafka 报名命令生产者、消费者与异步落库处理
7. MySQL 报名表 / 候补表写入逻辑与数据库唯一约束兜底
8. 不可恢复失败补偿、瞬时失败重试保留点与后续对账扩展说明

影响模块：
- `joinup-signup`
- `joinup-common`
- `joinup-waitlist`

相关文件：
- `docs/phase-6-signup-module.md`
- `joinup-signup/src/main/java/com/joinup/signup/service/impl/SignupServiceImpl.java`
- `joinup-signup/src/main/java/com/joinup/signup/domain/SignupActivityCacheService.java`
- `joinup-signup/src/main/resources/lua/signup_apply.lua`
### 第7段（已完成）
完成时间：2026-04-02

本段目标：实现 `joinup-waitlist` 候补模块，打通“正式名额释放 -> 候补自动补位 -> 补位通知 -> 超时顺延”的闭环。
已完成项：
1. `joinup-waitlist` 模块的 Entity / Enum / Mapper / Service / Controller / Redis 队列服务 / 领域服务 / 事件监听 / 定时任务骨架
2. 候补 Redis 结构设计落地：活动候补队列 ZSet、候补状态 Hash、全局补位超时 ZSet、活动级补位锁 Key
3. 正式席位释放后按先到先得推进下一位候补，并使用 Redisson 锁避免并发补位冲突
4. 候补补位提醒通知落表到 `notify_message`
5. 候补补位超时扫描与顺延机制落地
6. `joinup-signup` 与 `joinup-waitlist` 通过领域事件衔接：
- `WaitlistJoinedEvent`
- `WaitlistCanceledEvent`
- `FormalSignupCanceledEvent`
- `WaitlistPromotionOfferedEvent`
- `WaitlistPromotionExpiredEvent`
7. 报名模块已补齐候补补位占位 / 超时释放正式席位的联动逻辑

影响模块：
- `joinup-waitlist`
- `joinup-signup`
- `joinup-common`
- `joinup-notify`
- `joinup-boot`
- `joinup-infra`

相关文件：
- `docs/phase-7-waitlist-module.md`
- `joinup-waitlist/src/main/java/com/joinup/waitlist/domain/WaitlistPromotionDomainService.java`
- `joinup-waitlist/src/main/java/com/joinup/waitlist/domain/WaitlistRedisQueueService.java`
- `joinup-signup/src/main/java/com/joinup/signup/domain/WaitlistPromotionSignupCoordinator.java`
- `joinup-infra/src/main/resources/sql/joinup_waitlist_phase7.sql`

### 第8段（已完成）
完成时间：2026-04-02

本段目标：实现活动报名截止后的自动成团 / 流局判定、状态日志写入、分布式锁调度控制以及事务后 Kafka 事件发送骨架。
已完成项：
1. `joinup-activity` 内新增成团 / 流局领域规则、状态日志领域服务、分布式锁服务、定时扫描任务、结算服务实现
2. 定时扫描到报名截止时间已到且状态仍为 `SIGNUP_OPEN / FULL / WAITLIST_OPEN` 的活动
3. 根据 `current_participants >= min_group_size` 自动判定 `GROUP_SUCCESS / GROUP_FAILED`
4. 结算后统一写入 `activity_status_log`
5. 使用 Redisson 全局扫描锁 + 活动级锁避免多实例重复执行
6. 通过 `ActivitySettlementCompletedEvent` 在事务提交后发送 Kafka 结算结果事件
7. 新增活动结算配置项与结算扫描查询方法

影响模块：
- `joinup-activity`
- `joinup-common`
- `joinup-boot`

相关文件：
- `docs/phase-8-activity-settlement.md`
- `joinup-activity/src/main/java/com/joinup/activity/service/impl/ActivitySettlementServiceImpl.java`
- `joinup-activity/src/main/java/com/joinup/activity/scheduler/ActivitySettlementScheduler.java`
- `joinup-common/src/main/java/com/joinup/common/event/activity/ActivitySettlementCompletedEvent.java`
### 第9段（已完成）
完成时间：2026-04-02

本段目标：实现 `joinup-credit` 用户信用模块，覆盖信用规则、信用变更服务、信用限制校验、用户信用查询与管理员人工调整骨架。
已完成项：
1. `joinup-credit` 模块的 DTO / VO / Mapper / Service / Controller / Domain 规则骨架
2. 用户信用详情查询接口 `GET /api/credit/my`
3. 管理员人工调整接口骨架 `POST /api/admin/credit/adjust/{userId}`
4. 信用分变更服务：守约加分、允许时间内取消、临时取消扣分、爽约扣分、人工调整
5. 信用限制校验：限制报名热门活动、限制创建活动、降低候补优先级
6. 面向 `joinup-signup / joinup-waitlist / joinup-activity` 的服务衔接点
7. `joinup.credit.*` 配置项与信用错误码补充

影响模块：
- `joinup-credit`
- `joinup-common`
- `joinup-boot`

相关文件：
- `docs/phase-9-credit-module.md`
- `joinup-credit/src/main/java/com/joinup/credit/service/impl/CreditServiceImpl.java`
- `joinup-credit/src/main/java/com/joinup/credit/domain/CreditRestrictionChecker.java`
- `joinup-credit/src/main/java/com/joinup/credit/controller/CreditController.java`
### 第10段（已完成）
完成时间：2026-04-02

本段目标：实现 `joinup-notify` 通知模块，并补齐基于 Kafka 的事件驱动通知设计。
已完成项：
1. `notify_message` 对应的 Entity / Mapper / 状态枚举 / 渠道枚举 / 通知类型枚举
2. 通知分页接口 `GET /api/notify/page`
3. 通知已读接口 `POST /api/notify/read/{id}`
4. `signup-created / signup-canceled / waitlist-promoted / activity-group-success / activity-group-failed / credit-changed / notify-send` Kafka 主题常量与事件载荷
5. 上游业务事件消费者、统一通知发送命令生产者、通知发送命令消费者骨架
6. 站内信落库逻辑与短信 / 邮件 / 小程序通知发送器预留接口
7. 通知模块阶段文档 `docs/phase-10-notify-module.md`

影响模块：
- `joinup-notify`
- `joinup-common`
- `joinup-waitlist`（兼容旧通知枚举名引用）

相关文件：
- `docs/phase-10-notify-module.md`
- `joinup-notify/src/main/java/com/joinup/notify/service/impl/NotifyServiceImpl.java`
- `joinup-notify/src/main/java/com/joinup/notify/consumer/NotifyBusinessEventConsumer.java`
- `joinup-notify/src/main/java/com/joinup/notify/producer/NotifySendEventProducer.java`