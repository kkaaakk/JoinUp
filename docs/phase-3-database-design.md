# JoinUp 第3段：数据库设计与实体骨架

## 1. 建表 SQL
- 文件位置：`joinup-infra/src/main/resources/sql/joinup_schema_v1.sql`
- 覆盖表：
  - `user`
  - `user_profile`
  - `activity`
  - `activity_tag`
  - `activity_signup`
  - `activity_waitlist`
  - `activity_status_log`
  - `user_credit_record`
  - `notify_message`
  - `activity_report`
  - `operation_log`

## 2. 字段说明（按表）

### 2.1 `user`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| username | varchar(64) | 用户名，唯一 |
| phone | varchar(20) | 手机号，唯一 |
| email | varchar(128) | 邮箱，唯一 |
| password_hash | varchar(255) | 密码哈希 |
| status | tinyint | 用户状态 |
| credit_score | int | 用户信用分 |
| last_login_at | datetime(3) | 最后登录时间 |
| created_at/updated_at | datetime(3) | 审计时间 |
| created_by/updated_by | bigint | 审计操作人 |
| deleted | tinyint | 逻辑删除 |
| version | int | 乐观锁 |

### 2.2 `user_profile`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 用户ID，唯一 |
| nickname | varchar(64) | 昵称 |
| avatar_url | varchar(512) | 头像 |
| gender | tinyint | 性别 |
| birthday | date | 生日 |
| school_name | varchar(128) | 学校 |
| major | varchar(128) | 专业 |
| bio | varchar(500) | 签名 |
| city | varchar(64) | 城市 |
| created_at/updated_at/created_by/updated_by/deleted | - | 审计与逻辑删除 |

### 2.3 `activity`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| organizer_id | bigint | 发起人 |
| title | varchar(128) | 活动标题 |
| description | varchar(2000) | 活动描述 |
| category | varchar(32) | 类别 |
| location_text | varchar(255) | 地点 |
| start_time/end_time | datetime(3) | 活动起止时间 |
| signup_deadline | datetime(3) | 报名截止时间 |
| max_participants | int | 最大人数 |
| min_group_size | int | 成团最小人数 |
| current_participants | int | 当前报名人数 |
| waitlist_count | int | 候补人数 |
| allow_waitlist | tinyint | 是否允许候补 |
| waitlist_limit | int | 候补上限 |
| status | tinyint | 活动状态 |
| view_count | bigint | 浏览量 |
| heat_score | int | 活动热度 |
| cancel_reason | varchar(255) | 取消原因 |
| reviewed_by/reviewed_at/review_remark | bigint/datetime/varchar | 审核信息 |
| created_at/updated_at/created_by/updated_by/deleted/version | - | 审计、逻辑删除、乐观锁 |

### 2.4 `activity_tag`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| activity_id | bigint | 活动ID |
| tag_name | varchar(32) | 标签名 |
| tag_type | tinyint | 标签类型 |
| created_at/updated_at/created_by/updated_by/deleted | - | 审计与逻辑删除 |

### 2.5 `activity_signup`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| activity_id | bigint | 活动ID |
| user_id | bigint | 用户ID |
| status | tinyint | 报名状态 |
| source | tinyint | 来源（直接报名/候补补位） |
| signup_time | datetime(3) | 报名时间 |
| cancel_time | datetime(3) | 取消时间 |
| cancel_reason | varchar(255) | 取消原因 |
| created_at/updated_at/created_by/updated_by/deleted/version | - | 审计、逻辑删除、乐观锁 |

### 2.6 `activity_waitlist`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| activity_id | bigint | 活动ID |
| user_id | bigint | 用户ID |
| queue_no | int | 候补顺序（越小越靠前） |
| status | tinyint | 候补状态 |
| joined_at | datetime(3) | 入队时间 |
| promoted_at | datetime(3) | 补位时间 |
| expired_at | datetime(3) | 失效时间 |
| created_at/updated_at/created_by/updated_by/deleted/version | - | 审计、逻辑删除、乐观锁 |

### 2.7 `activity_status_log`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| activity_id | bigint | 活动ID |
| from_status/to_status | tinyint | 状态流转 |
| reason | varchar(255) | 变更原因 |
| operator_id | bigint | 操作人 |
| occurred_at | datetime(3) | 发生时间 |
| created_at/updated_at/created_by/updated_by/deleted | - | 审计与逻辑删除 |

### 2.8 `user_credit_record`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 用户ID |
| change_type | tinyint | 信用变更类型 |
| delta_score | int | 变更值 |
| before_score/after_score | int | 变更前后分数 |
| reason | varchar(255) | 变更原因 |
| related_activity_id | bigint | 关联活动 |
| related_signup_id | bigint | 关联报名 |
| created_at/updated_at/created_by/updated_by/deleted | - | 审计与逻辑删除 |

### 2.9 `notify_message`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 接收人 |
| message_type | tinyint | 消息类型 |
| channel | tinyint | 渠道 |
| title/content | varchar | 标题内容 |
| biz_type/biz_id | varchar/bigint | 业务关联 |
| status | tinyint | 通知状态 |
| send_time/read_time | datetime(3) | 发送/已读时间 |
| fail_reason | varchar(255) | 失败原因 |
| created_at/updated_at/created_by/updated_by/deleted | - | 审计与逻辑删除 |

### 2.10 `activity_report`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| activity_id | bigint | 活动ID |
| reporter_user_id | bigint | 举报人 |
| reported_user_id | bigint | 被举报人 |
| report_type | tinyint | 举报类型 |
| reason | varchar(500) | 举报内容 |
| evidence_urls | varchar(2000) | 证据 |
| status | tinyint | 处理状态 |
| handler_user_id | bigint | 处理人 |
| handle_result | varchar(500) | 处理结果 |
| handled_at | datetime(3) | 处理时间 |
| created_at/updated_at/created_by/updated_by/deleted | - | 审计与逻辑删除 |

### 2.11 `operation_log`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| operator_id/operator_role | bigint/varchar | 操作人 |
| module_name/operation_type | varchar | 模块与操作 |
| biz_type/biz_id | varchar/bigint | 业务对象 |
| request_id/request_path/request_method/request_ip/user_agent | varchar | 请求上下文 |
| operation_result | tinyint | 操作结果 |
| error_message | varchar(500) | 错误信息 |
| extra_data | json | 扩展字段 |
| operation_time | datetime(3) | 操作时间 |
| created_at/updated_at/created_by/updated_by/deleted | - | 审计与逻辑删除 |

## 3. 索引设计建议
1. 高并发报名核心：
   - `activity_signup` 使用唯一索引 `uk_signup_activity_user(activity_id, user_id)` 防重报名。
   - `activity_waitlist` 使用唯一索引 `uk_waitlist_activity_user(activity_id, user_id)` 防重候补。
   - `activity_waitlist` 使用唯一索引 `uk_waitlist_activity_queue(activity_id, queue_no)` 保证候补顺位不冲突。
2. 截止判定与状态扫描：
   - `activity(status, signup_deadline)` 支撑定时任务扫描。
3. 补位流程：
   - `activity_waitlist(activity_id, status, queue_no)` 快速取首位候补。
4. 信用与风控：
   - `user(status, credit_score)` 支撑低信用限制策略。
   - `user_credit_record(user_id, created_at)` 支撑信用流水查询。
5. 通知投递：
   - `notify_message(status, send_time)` 支撑待发送扫描。
6. 运维审计：
   - `operation_log(module_name, operation_time)` 和 `(operator_id, operation_time)` 支撑追踪。

## 4. 关键约束
1. 报名去重：`activity_signup(activity_id, user_id)` 唯一。
2. 候补去重：`activity_waitlist(activity_id, user_id)` 唯一。
3. 候补顺位唯一：`activity_waitlist(activity_id, queue_no)` 唯一。
4. 用户资料一对一：`user_profile(user_id)` 唯一。
5. 活动标签去重：`activity_tag(activity_id, tag_name, deleted)` 唯一。

## 5. 枚举字段设计建议
- 推荐使用 `tinyint/int` 存储枚举编码，Java 端使用 Enum 管理：
  - `user.status` -> `UserStatusEnum`
  - `activity.status` -> `ActivityStatusEnum`
  - `activity_signup.status` -> `SignupStatusEnum`
  - `activity_waitlist.status` -> `WaitlistStatusEnum`
  - `notify_message.status` -> `NotifyStatusEnum`
  - `user_credit_record.change_type` -> `CreditChangeTypeEnum`
  - `activity_report.status` -> `ReportStatusEnum`
  - `operation_log.operation_result` -> `OperationResultEnum`

## 6. 逻辑删除字段设计
- 统一字段：`deleted TINYINT NOT NULL DEFAULT 0`
- 语义：`0=未删除，1=已删除`
- MyBatis Plus：实体继承 `BaseEntity`，`deleted` 使用 `@TableLogic`。

## 7. 审计字段设计
- 统一字段：
  - `created_at DATETIME(3)`
  - `updated_at DATETIME(3)`
  - `created_by BIGINT`
  - `updated_by BIGINT`
- 填充策略：由 `AuditMetaObjectHandler` 自动填充。

## 8. 模块归属
- `joinup-user`：`user`、`user_profile`
- `joinup-activity`：`activity`、`activity_tag`、`activity_status_log`、`activity_report`
- `joinup-signup`：`activity_signup`
- `joinup-waitlist`：`activity_waitlist`
- `joinup-credit`：`user_credit_record`
- `joinup-notify`：`notify_message`
- `joinup-admin`：`operation_log`
- `joinup-infra`：DDL SQL 与基础设施配置
