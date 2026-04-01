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
