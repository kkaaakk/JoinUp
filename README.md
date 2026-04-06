# JoinUp Backend

## 项目简介
JoinUp（组个局）是一个面向校园和社区的线下临时活动组局平台后端。

它要解决的不是电商下单，也不是传统预约，而是“临时发起活动、快速抢位、候补补位、成团流局、信用约束、通知触达”这一整套高时效业务闭环。

当前仓库采用：
1. Java 17
2. Spring Boot 3.x
3. Maven 多模块
4. 模块化单体架构

## 核心业务能力
1. 用户注册、登录、JWT 鉴权、个人资料维护
2. 活动创建、编辑、取消、审核、分页查询
3. 高并发报名抢位、幂等防重、异步落库
4. 满员后候补排队、正式用户取消后自动补位、超时顺延
5. 报名截止后的自动成团/流局结算
6. 信用分变更、低信用限制、后台人工干预
7. 站内通知、Kafka 事件驱动通知链路
8. 管理后台审核、举报处理、热门活动监控、风险用户监控、操作日志

## 技术栈
### 基础框架
1. Java 17
2. Spring Boot 3.x
3. Spring Web
4. Spring Validation
5. Spring Security + JWT
6. Spring Scheduler

### 数据与中间件
1. MySQL 8.x
2. Redis
3. Redisson
4. Kafka
5. MyBatis Plus

### 开发效率与测试
1. Lombok
2. MapStruct
3. Knife4j / OpenAPI
4. JUnit 5
5. Mockito

## 架构说明
项目采用模块化单体，而不是微服务。

这样做的原因是：
1. 业务还在快速演进阶段，单体更利于收敛规则
2. 报名、候补、信用、通知之间耦合很强，拆太早会增加复杂度
3. 通过模块边界、领域事件、服务接口先把结构做对，后续再按热点和团队边界拆分更稳

### 分层约定
每个业务模块统一采用以下分层：
1. `controller`
2. `service`
3. `domain`
4. `mapper`
5. `entity`
6. `dto`
7. `vo`

## Maven 模块
### 启动与基础
1. `joinup-boot`
   说明：启动模块，装配全项目
2. `joinup-common`
   说明：通用返回结构、异常、事件、上下文、常量
3. `joinup-infra`
   说明：MySQL、Redis、Kafka、MyBatis Plus、OpenAPI、Security 等基础设施

### 业务模块
1. `joinup-user`
   说明：用户、登录、资料、鉴权
2. `joinup-activity`
   说明：活动创建、编辑、审核、详情、成团流局
3. `joinup-signup`
   说明：高并发报名抢位、取消、异步落库
4. `joinup-waitlist`
   说明：候补排队、自动补位、超时顺延、候补查询
5. `joinup-credit`
   说明：信用规则、信用分变更、限制校验
6. `joinup-notify`
   说明：站内通知、通知事件消费、通知分页
7. `joinup-admin`
   说明：后台审核、举报处理、风险监控、操作日志

## 当前代码现状
### 已完成的核心闭环
1. 用户认证闭环
2. 活动创建与审核闭环
3. 报名抢位主链路
4. 候补补位闭环
5. 成团/流局结算闭环
6. 信用约束骨架
7. 通知模块骨架与消费入口
8. 后台治理骨架

### 仍建议继续补强的部分
1. 业务 Topic 的上游 Producer 全量接入
2. Testcontainers 端到端集成测试
3. 正式 RBAC 与更细粒度后台权限
4. 热门榜单与推荐链路的真实更新任务

## 快速启动
### 1. 准备依赖
本项目需要以下中间件：
1. MySQL 8.x
2. Redis
3. Kafka

### 2. 初始化数据库
先执行：
1. [joinup_schema_v1.sql](/C:/JoinUp/joinup-infra/src/main/resources/sql/joinup_schema_v1.sql)
2. [joinup_waitlist_phase7.sql](/C:/JoinUp/joinup-infra/src/main/resources/sql/joinup_waitlist_phase7.sql)

### 3. 检查配置
配置文件在：
[application.yml](/C:/JoinUp/joinup-boot/src/main/resources/application.yml)

需要重点确认：
1. MySQL 地址、账号、密码
2. Redis 地址
3. Kafka 地址
4. JWT 密钥

### 4. 编译项目
```bash
mvn -q "-Dmaven.repo.local=.m2" -DskipTests compile
```

### 5. 启动应用
启动类在：
[JoinUpApplication.java](/C:/JoinUp/joinup-boot/src/main/java/com/joinup/boot/JoinUpApplication.java)

## 关键文档索引
1. [phase-3-database-design.md](/C:/JoinUp/docs/phase-3-database-design.md)
2. [phase-4-user-module.md](/C:/JoinUp/docs/phase-4-user-module.md)
3. [phase-5-activity-module.md](/C:/JoinUp/docs/phase-5-activity-module.md)
4. [phase-6-signup-module.md](/C:/JoinUp/docs/phase-6-signup-module.md)
5. [phase-7-waitlist-module.md](/C:/JoinUp/docs/phase-7-waitlist-module.md)
6. [phase-8-activity-settlement.md](/C:/JoinUp/docs/phase-8-activity-settlement.md)
7. [phase-9-credit-module.md](/C:/JoinUp/docs/phase-9-credit-module.md)
8. [phase-10-notify-module.md](/C:/JoinUp/docs/phase-10-notify-module.md)
9. [phase-11-admin-module.md](/C:/JoinUp/docs/phase-11-admin-module.md)
10. [phase-12-project-wrap-up.md](/C:/JoinUp/docs/phase-12-project-wrap-up.md)
11. [joinup-project-study-guide.md](/C:/JoinUp/docs/joinup-project-study-guide.md)

## 关键代码入口
1. 用户模块
   [UserController.java](/C:/JoinUp/joinup-user/src/main/java/com/joinup/user/controller/UserController.java)
2. 活动模块
   [ActivityController.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/controller/ActivityController.java)
3. 报名模块
   [SignupServiceImpl.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/service/impl/SignupServiceImpl.java)
4. 候补模块
   [WaitlistPromotionDomainService.java](/C:/JoinUp/joinup-waitlist/src/main/java/com/joinup/waitlist/domain/WaitlistPromotionDomainService.java)
5. 成团流局模块
   [ActivitySettlementServiceImpl.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/service/impl/ActivitySettlementServiceImpl.java)
6. 信用模块
   [CreditServiceImpl.java](/C:/JoinUp/joinup-credit/src/main/java/com/joinup/credit/service/impl/CreditServiceImpl.java)
7. 通知模块
   [NotifyServiceImpl.java](/C:/JoinUp/joinup-notify/src/main/java/com/joinup/notify/service/impl/NotifyServiceImpl.java)
8. 管理后台模块
   [AdminReportServiceImpl.java](/C:/JoinUp/joinup-admin/src/main/java/com/joinup/admin/service/impl/AdminReportServiceImpl.java)

## 分段交付记录
### 第1段
1. 项目整体架构设计
2. Maven 多模块划分
3. 模块职责说明
4. 技术选型说明
5. package 结构建议
6. 后续分步计划

### 第2段
1. 项目骨架
2. 基础设施层
3. Spring Security + JWT 骨架
4. 全局异常与统一返回
5. Swagger / MyBatis Plus / 审计字段

### 第3段
1. 数据库表结构设计
2. 索引与约束设计
3. MyBatis Plus Entity 骨架
4. 枚举字段与逻辑删除设计

### 第4段
1. `joinup-user`
2. 用户注册、登录、鉴权
3. 个人资料查询与修改
4. 信用分查询

### 第5段
1. `joinup-activity`
2. 活动创建、修改、取消、详情、分页
3. 管理员审核接口
4. 活动状态流转骨架

### 第6段
1. `joinup-signup`
2. Redis 库存缓存
3. Lua 原子扣减
4. Kafka 异步落库
5. 幂等控制与结果查询

### 第7段
1. `joinup-waitlist`
2. 候补排队
3. 正式取消后自动补位
4. 超时未确认顺延
5. 候补查询接口

### 第8段
1. 成团 / 流局结算
2. 定时任务扫描
3. 状态日志写入
4. 分布式锁防重
5. 结算事件发送

### 第9段
1. `joinup-credit`
2. 信用分变更规则
3. 低信用限制校验
4. 用户信用查询
5. 管理员人工调整骨架

### 第10段
1. `joinup-notify`
2. 通知消息落库
3. 业务 Topic 与通知发送命令
4. 通知分页和已读接口

### 第11段
1. `joinup-admin`
2. 举报处理
3. 热门活动监控
4. 风险用户监控
5. 操作日志查询
6. RBAC 权限编码预留

### 第12段
1. Redis Key 汇总
2. Kafka 事件链路汇总
3. 高并发报名、候补补位、成团流局时序图
4. 单元测试与集成测试建议
5. 接口文档组织建议
6. README 初稿
7. 后续扩展能力建议

## 后续建议
1. 优先把业务 Topic 的 Producer 真正补齐
2. 再补 Testcontainers 集成测试
3. 最后再做实时能力、推荐、聊天室、地图、海报生成等扩展
