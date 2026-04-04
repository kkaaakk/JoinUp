# JoinUp 项目吃透手册

## 文档定位
这份文档不是单纯的阶段回顾，而是给你用来“吃透项目”的学习手册。

它会回答四类问题：
1. 每一段到底做了什么
2. 每一段解决了什么业务问题
3. 为什么用了这些技术，而不是别的方案
4. 你现在应该先看哪些代码，怎么把整个项目串起来理解

## 建议你先建立的全局认识
在真正逐段细看之前，你先抓住 JoinUp 的 5 条主线：

1. 用户主线
   用户如何注册、登录、带着 JWT 访问系统

2. 活动主线
   活动如何创建、审核、开放报名、截止结算、最终进入成团或流局

3. 报名主线
   热门活动下如何在高并发场景里做到不超卖、不重复报名、最终一致

4. 候补主线
   正式名额满后如何排队、正式用户取消后如何自动补位、超时未确认如何顺延

5. 信用与通知主线
   用户行为如何影响信用分，信用分如何反向约束报名与创建，关键状态变化如何通过通知触达用户

如果你把这 5 条线看明白，这个项目就已经吃透了大半。

## 推荐阅读顺序
如果你现在准备系统性理解项目，建议按这个顺序读：

1. 先读 [README.md](/C:/JoinUp/README.md)
   目标：建立项目全貌，知道有哪些模块、解决什么问题

2. 再读 [phase-12-project-wrap-up.md](/C:/JoinUp/docs/phase-12-project-wrap-up.md)
   目标：快速了解 Redis、Kafka、时序图、测试建议、后续演进方向

3. 然后按业务主链读代码：
   1. `joinup-user`
   2. `joinup-activity`
   3. `joinup-signup`
   4. `joinup-waitlist`
   5. `joinup-activity` 的结算逻辑
   6. `joinup-credit`
   7. `joinup-notify`
   8. `joinup-admin`

4. 最后再回头看数据库和基础设施：
   1. [phase-3-database-design.md](/C:/JoinUp/docs/phase-3-database-design.md)
   2. `joinup-infra`
   3. `application.yml`

这样读的好处是：先抓主线，再补细节，不会一上来就陷进配置和表结构里。

---

## 第1段：整体架构设计

### 这一段干了什么
第1段没有直接写代码，而是先把整个项目的架构边界、模块划分、技术选型和后续开发顺序定下来。

### 解决了什么问题
在 JoinUp 这种业务里，最大的风险不是“少写一个接口”，而是一开始就把边界做乱了。

如果不先做架构设计，后面很容易出现这些问题：
1. 报名、候补、活动、信用的职责缠在一起
2. 状态流转散在 Controller 里
3. 后面一旦要扩展成团、通知、风控时，改动面会非常大

第1段的价值就是先把“骨架和边界”定住，让后面每一段都是往正确方向加东西，而不是边写边返工。

### 用了什么技术或方法
1. 模块化单体架构
2. 分层约定：`controller/service/domain/mapper/entity/dto/vo`
3. 模块间通过领域事件和服务接口解耦

### 为什么这样设计
JoinUp 当前的业务耦合度很高，特别是：
1. 活动状态会影响报名
2. 报名取消会影响候补
3. 爽约会影响信用
4. 成团/流局会触发通知和后续收口

如果一开始就拆成微服务，只会把复杂度提前放大。

所以第1段选了“模块化单体”而不是“微服务”。这不是妥协，而是更适合当前阶段的工程决策。

### 你现在读这段时应该抓什么
你要记住的是：
1. 整个项目是按“业务边界”拆模块，不是按技术框架拆模块
2. 每个模块都想尽量成为未来可独立拆分的业务单元
3. 领域事件是后面串联报名、候补、结算、通知的重要机制

---

## 第2段：项目骨架与基础设施层

### 这一段干了什么
第2段把整个多模块 Maven 工程、启动模块、基础配置、Spring Security、JWT、统一异常处理、统一返回结构、MyBatis Plus、OpenAPI、审计字段这些基础能力全部搭起来了。

### 解决了什么问题
这段解决的是“项目能不能稳定地承载后续所有业务模块”的问题。

没有这一段，后面每一段都会陷入重复造轮子：
1. 每个模块自己处理异常
2. 每个模块自己定返回格式
3. 每个模块自己拼安全配置
4. 每个模块自己声明基础依赖

第2段把这些横切问题一次性收掉，后面业务模块就能只关注业务本身。

### 用了什么技术
1. Maven 多模块
2. Spring Boot 3
3. Spring Security + JWT
4. MyBatis Plus
5. Redis / Kafka 基础配置
6. Swagger / Knife4j
7. 全局异常处理器
8. 统一 `Result<T>`
9. 通用审计基类 `BaseEntity`

### 为什么这样选
1. `Spring Security + JWT`
   适合前后端分离，认证链路标准化，可扩展 RBAC
2. `MyBatis Plus`
   适合当前这类表结构明确、CRUD 较多、又需要保留 SQL 可控性的项目
3. `Result<T> + 全局异常处理`
   让前端联调和接口文档更稳定，不会每个接口各说各话
4. `BaseEntity`
   统一 `created_at / updated_at / deleted`，后面所有表都能继承，不用每次重写

### 关键文件
1. [pom.xml](/C:/JoinUp/pom.xml)
2. [JoinUpApplication.java](/C:/JoinUp/joinup-boot/src/main/java/com/joinup/boot/JoinUpApplication.java)
3. [application.yml](/C:/JoinUp/joinup-boot/src/main/resources/application.yml)
4. [SecurityConfig.java](/C:/JoinUp/joinup-infra/src/main/java/com/joinup/infrastructure/security/SecurityConfig.java)
5. [JwtTokenProvider.java](/C:/JoinUp/joinup-infra/src/main/java/com/joinup/infrastructure/security/JwtTokenProvider.java)
6. [GlobalExceptionHandler.java](/C:/JoinUp/joinup-infra/src/main/java/com/joinup/infrastructure/web/GlobalExceptionHandler.java)
7. [Result.java](/C:/JoinUp/joinup-common/src/main/java/com/joinup/common/result/Result.java)

### 你读这段时应该抓什么
你要先接受一个事实：
后面所有业务代码都建立在第2段的基础设施之上。

所以如果你看后面代码时碰到这些问题：
1. 当前用户是谁
2. 异常为什么能统一返回
3. Mapper 为什么直接能用
4. OpenAPI 为什么自动出文档

答案大多都在第2段。

---

## 第3段：数据库设计与实体骨架

### 这一段干了什么
第3段完成了整个项目的核心表设计、索引设计、逻辑删除设计、审计字段设计，并为各模块生成了对应的 Entity 和枚举。

### 解决了什么问题
这一段解决的是“后面所有业务逻辑是否有一个合理的数据底座”的问题。

如果数据库设计不合理，后面会出现：
1. 报名和候补混在一张表里，逻辑很难演进
2. 成团/流局没有状态日志，出了问题无法审计
3. 信用记录没有前后分值快照，后面很难追责
4. 高并发下缺唯一约束，最终会出现重复数据

### 用了什么技术或设计方法
1. MySQL 8.x 建表 DDL
2. MyBatis Plus Entity
3. 唯一索引与业务约束并存
4. 逻辑删除字段 `deleted`
5. 审计字段统一落基类

### 这段真正解决的关键点
1. `activity_signup(activity_id, user_id)` 唯一索引
   这是报名防重复的数据库最终兜底
2. `activity_waitlist(activity_id, user_id)` 唯一索引
   防止同一个用户被重复塞进候补
3. `activity_waitlist(activity_id, queue_no)` 唯一索引
   保证候补顺序不会冲掉
4. `user.credit_score` + `user_credit_record`
   一个管当前值，一个管变化历史，兼顾高频读和审计追踪
5. `activity_status_log`
   后面成团、流局、审核、取消的状态变化都有地方落日志

### 关键文件
1. [joinup_schema_v1.sql](/C:/JoinUp/joinup-infra/src/main/resources/sql/joinup_schema_v1.sql)
2. [phase-3-database-design.md](/C:/JoinUp/docs/phase-3-database-design.md)

### 你读这段时应该抓什么
你要先把这些表理解成“事实来源”：
1. 活动事实来源：`activity`
2. 正式报名事实来源：`activity_signup`
3. 候补事实来源：`activity_waitlist`
4. 信用变化事实来源：`user_credit_record`
5. 通知事实来源：`notify_message`
6. 后台审计事实来源：`operation_log`

后面 Redis 和 Kafka 都是在围绕这些事实来源做性能优化和异步解耦，而不是替代它们。

---

## 第4段：用户模块 `joinup-user`

### 这一段干了什么
第4段实现了：
1. 用户注册
2. 用户登录
3. JWT 生成与校验
4. 获取当前用户资料
5. 修改个人资料
6. 查询当前用户信用分

### 解决了什么问题
这段解决的是“系统怎么知道你是谁，你有没有资格调用受保护接口”。

如果没有这一段，后面活动创建、报名、后台操作都无法成立，因为所有这些行为都必须依赖“当前登录用户”。

### 用了什么技术
1. Spring Security
2. JWT
3. BCrypt 密码加密
4. `@AuthenticationPrincipal LoginUser`
5. 用户状态校验

### 为什么这样选
1. `BCrypt`
   密码绝不能明文存储，这是最基本安全要求
2. `JWT`
   很适合前后端分离，不需要服务端会话粘连
3. `LoginUser`
   不把完整用户实体塞进安全上下文，而是用轻量快照，后面扩展权限也更干净

### 关键代码
1. [UserController.java](/C:/JoinUp/joinup-user/src/main/java/com/joinup/user/controller/UserController.java)
2. [UserServiceImpl.java](/C:/JoinUp/joinup-user/src/main/java/com/joinup/user/service/impl/UserServiceImpl.java)
3. [JwtAuthenticationFilter.java](/C:/JoinUp/joinup-infra/src/main/java/com/joinup/infrastructure/security/JwtAuthenticationFilter.java)
4. [JwtTokenProvider.java](/C:/JoinUp/joinup-infra/src/main/java/com/joinup/infrastructure/security/JwtTokenProvider.java)

### 你读这段时应该抓什么
你要看明白这条认证链路：
1. 登录接口验证密码
2. 登录成功签发 JWT
3. 请求进来后过滤器解析 JWT
4. `LoginUser` 放入 Spring Security 上下文
5. Controller 用 `@AuthenticationPrincipal` 直接拿当前用户

后面几乎所有模块都依赖这条链路。

---

## 第5段：活动模块 `joinup-activity`

### 这一段干了什么
第5段实现了：
1. 创建活动
2. 修改活动
3. 查询详情
4. 分页查询
5. 取消活动
6. 管理员审核活动
7. 活动状态流转骨架

### 解决了什么问题
这段解决的是“平台上的活动对象如何被创建、审核、开放、取消”的问题。

JoinUp 不是一个单纯的报名系统，它首先是一个“活动驱动系统”。
没有第5段，报名模块就失去了核心载体。

### 用了什么技术和设计
1. `ActivityStatusEnum`
2. `ActivityStatusFlow`
3. `ActivityPermissionChecker`
4. `ActivityServiceImpl`
5. `ActivityTag` 批量替换逻辑

### 为什么状态机要集中管理
活动状态很容易失控，尤其是后面接入：
1. 报名中
2. 已满员
3. 候补开放
4. 成团成功
5. 成团失败
6. 已取消

如果这些状态流转散在多个 Service 甚至 Controller 里，后面非常容易写乱。

所以第5段把状态流转集中在 `ActivityStatusFlow`，这是为了让后面的报名、结算、后台审核都能基于同一套状态语义工作。

### 关键代码
1. [ActivityServiceImpl.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/service/impl/ActivityServiceImpl.java)
2. [ActivityStatusFlow.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/domain/ActivityStatusFlow.java)
3. [AdminActivityController.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/controller/AdminActivityController.java)

### 你读这段时应该抓什么
你要重点理解两件事：
1. 活动状态不是普通字段，而是后面整个业务链路的开关
2. 审核、取消、开放报名这些动作本质上都是在推活动状态机

---

## 第6段：报名抢位模块 `joinup-signup`

### 这一段干了什么
第6段实现了项目里最核心的高并发报名链路：
1. Redis 缓存正式剩余名额
2. Lua 原子扣减
3. 幂等防重
4. 满员后自动切候补
5. Kafka 异步落库
6. 失败补偿
7. 报名结果查询

### 解决了什么问题
这是全项目技术难度最高的一段。

它解决的是：
在热门活动只剩少量名额时，大量用户同时点报名，系统如何做到：
1. 不超卖
2. 不重复报名
3. 不把数据库打垮
4. 最终数据一致

### 为什么不用“直接扣数据库库存”
因为那样在热门活动场景下会带来：
1. 行锁竞争严重
2. 接口 RT 飙升
3. 容易把数据库拖成瓶颈

所以第6段用了经典的“Redis 预占 + Kafka 异步落库 + 数据库唯一约束兜底”的组合方案。

### 用了什么技术
1. Redis
2. Lua 脚本
3. Kafka
4. MySQL 唯一索引
5. 双阶段状态：`PROCESSING -> 最终态`

### 核心设计思路
1. 先在 Redis 做原子预占
2. 预占成功后把命令投到 Kafka
3. 消费者异步落库
4. 成功后刷新 Redis 最终状态
5. 失败时补偿 Redis

### 关键代码
1. [SignupServiceImpl.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/service/impl/SignupServiceImpl.java)
2. [SignupActivityCacheService.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/domain/SignupActivityCacheService.java)
3. [SignupCommandHandler.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/domain/SignupCommandHandler.java)
4. [SignupCommandProducer.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/producer/SignupCommandProducer.java)
5. [signup_apply.lua](/C:/JoinUp/joinup-signup/src/main/resources/lua/signup_apply.lua)

### 你读这段时应该抓什么
你要重点搞清楚三层防线：
1. Redis 幂等
2. Kafka 重复消费幂等
3. MySQL 唯一约束兜底

只要这三层理解了，你就吃透了这个项目最关键的并发设计。

---

## 第7段：候补模块 `joinup-waitlist`

### 这一段干了什么
第7段把“名额满后怎么办”这件事真正做完整了：
1. 用户进入候补队列
2. 正式用户取消后自动补位
3. 给被补位用户发通知
4. 超时未确认则顺延下一位
5. 候补顺序可查询

### 解决了什么问题
如果没有第7段，JoinUp 只能做到“抢位”，做不到“活动席位利用最大化”。

这段解决的是：
1. 热门活动正式名额满后，后来的用户怎么办
2. 正式用户取消后，空出的名额如何公平分配
3. 候补补位过程如何避免并发冲突

### 用了什么技术
1. Redis ZSet 维护候补队列
2. Redisson 分布式锁
3. 候补状态机
4. 领域事件联动 `joinup-signup`
5. 定时任务扫描补位超时

### 为什么候补要单独成模块
因为候补不是报名的“附属字段”，它本身就是一套独立业务：
1. 有独立状态
2. 有独立顺序
3. 有独立超时逻辑
4. 有独立通知逻辑

所以第7段没有把它硬塞回 `joinup-signup`，而是拆成 `joinup-waitlist`。

### 关键代码
1. [WaitlistPromotionDomainService.java](/C:/JoinUp/joinup-waitlist/src/main/java/com/joinup/waitlist/domain/WaitlistPromotionDomainService.java)
2. [WaitlistRedisQueueService.java](/C:/JoinUp/joinup-waitlist/src/main/java/com/joinup/waitlist/domain/WaitlistRedisQueueService.java)
3. [WaitlistTimeoutScheduler.java](/C:/JoinUp/joinup-waitlist/src/main/java/com/joinup/waitlist/domain/WaitlistTimeoutScheduler.java)
4. [WaitlistPromotionSignupCoordinator.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/domain/WaitlistPromotionSignupCoordinator.java)

### 你读这段时应该抓什么
你要看懂的是：
1. 候补顺序事实来源在数据库，Redis 是性能索引
2. 补位动作必须串行化，否则容易一次补多个人
3. “候补超时”不是终点，而是重新触发下一轮补位

---

## 第8段：成团 / 流局与定时结算

### 这一段干了什么
第8段实现了：
1. 报名截止时间到达后的自动扫描
2. 根据最小成团人数判定成团或流局
3. 写入活动状态日志
4. 事务提交后发送结算事件
5. 分布式锁防止重复结算

### 解决了什么问题
JoinUp 不是一个无限期开放报名的平台，活动是有时间窗口的。

所以第8段解决的是：
1. 到截止时间后，活动该怎么自动收口
2. 人数够了是不是成团
3. 人数不够是不是流局
4. 多实例部署下会不会同一个活动被重复结算

### 用了什么技术
1. Spring Scheduler
2. Redisson 分布式锁
3. 结算规则类 `ActivitySettlementRule`
4. 状态日志服务 `ActivityStatusLogDomainService`
5. 事务后事件发布

### 为什么要两层锁
1. 全局扫描锁：防止多台机器一起扫同一批活动
2. 活动级锁：防止同一活动被重复结算

这是多实例定时任务里比较标准的做法。

### 关键代码
1. [ActivitySettlementScheduler.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/scheduler/ActivitySettlementScheduler.java)
2. [ActivitySettlementServiceImpl.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/service/impl/ActivitySettlementServiceImpl.java)
3. [ActivitySettlementRule.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/domain/ActivitySettlementRule.java)
4. [ActivityStatusLogDomainService.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/domain/ActivityStatusLogDomainService.java)

### 你读这段时应该抓什么
你要搞清楚的是：
1. 第5段的活动状态机在这里真正进入“自动流转”阶段
2. 第8段是活动生命周期从“开放报名”转向“最终结果”的关键节点
3. 结算事件是后面通知和下游收口的基础

---

## 第9段：信用模块 `joinup-credit`

### 这一段干了什么
第9段建立了完整的信用分骨架：
1. 信用规则
2. 信用分变更服务
3. 信用限制校验
4. 用户查询自己信用分
5. 管理员人工调整信用分
6. 给报名、活动、候补预留接入点

### 解决了什么问题
JoinUp 的核心业务不是“报名就完了”，而是“线下活动里人的信用很重要”。

这段解决的是：
1. 爽约如何惩罚
2. 临时取消如何扣分
3. 守约如何加分
4. 低信用用户如何被限制
5. 后台如何人工干预异常用户

### 用了什么技术和设计
1. `user.credit_score` 保存当前值
2. `user_credit_record` 保存变化明细
3. `CreditRuleEngine` 做规则计算
4. `CreditRestrictionChecker` 做限制判断
5. 配置化规则 `joinup.credit.*`

### 为什么当前值和明细表要同时保留
因为这是一个很典型的“高频读 + 审计追踪”场景：
1. 当前值放 `user` 表，报名时能快速判断
2. 明细表放历史记录，后面查原因、做风控、做运营报表都需要

### 关键代码
1. [CreditServiceImpl.java](/C:/JoinUp/joinup-credit/src/main/java/com/joinup/credit/service/impl/CreditServiceImpl.java)
2. [CreditRuleEngine.java](/C:/JoinUp/joinup-credit/src/main/java/com/joinup/credit/domain/CreditRuleEngine.java)
3. [CreditRestrictionChecker.java](/C:/JoinUp/joinup-credit/src/main/java/com/joinup/credit/domain/CreditRestrictionChecker.java)
4. [CreditController.java](/C:/JoinUp/joinup-credit/src/main/java/com/joinup/credit/controller/CreditController.java)

### 你读这段时应该抓什么
你要理解信用模块不是“用户资料的一个字段”，而是整个业务的风控约束层。

后面真正接得更完整时：
1. 报名前要校验信用
2. 候补排序可以参考信用
3. 活动创建前要校验信用
4. 爽约、守约、取消都要回写信用

---

## 第10段：通知模块 `joinup-notify`

### 这一段干了什么
第10段实现了：
1. `notify_message` 的实体、Mapper、分页和已读接口
2. 通知类型、通知状态、通知渠道枚举
3. Kafka Topic 常量汇总
4. 通知业务事件消费骨架
5. `notify-send` 统一发送命令链路
6. 站内信落库逻辑
7. 短信、邮件、小程序通知接口预留

### 解决了什么问题
这段解决的是“业务变化怎么传达到用户”这个问题。

JoinUp 很多状态变化都需要通知用户：
1. 报名成功
2. 候补补位
3. 成团
4. 流局
5. 信用变化

如果没有通知模块，这些业务虽然在数据库里完成了，但用户侧会很割裂。

### 为什么要把通知设计成事件驱动
因为通知不应该侵入主业务事务。

如果报名、候补、信用、结算这些主流程都同步调用通知发送：
1. 会拉长主链路 RT
2. 会增加耦合
3. 通知失败会反向污染主业务

所以第10段把通知设计成：
“业务事件 -> 通知命令 -> 通知分发”。

### 用了什么技术
1. Kafka Topic 拆分
2. `NotifyBusinessEventConsumer`
3. `NotifySendEventProducer`
4. `NotifySendConsumer`
5. `NotifyServiceImpl`

### 当前真正完成到了什么程度
这里很重要：
1. 通知模块本身已经能工作
2. 通知消费骨架已经齐了
3. 但部分业务 Topic 的上游 Producer 仍然需要继续补齐

也就是说，第10段完成的是“通知基础设施和消费入口”，不是所有通知业务都已经彻底接通。

### 关键代码
1. [NotifyBusinessEventConsumer.java](/C:/JoinUp/joinup-notify/src/main/java/com/joinup/notify/consumer/NotifyBusinessEventConsumer.java)
2. [NotifySendConsumer.java](/C:/JoinUp/joinup-notify/src/main/java/com/joinup/notify/consumer/NotifySendConsumer.java)
3. [NotifySendEventProducer.java](/C:/JoinUp/joinup-notify/src/main/java/com/joinup/notify/producer/NotifySendEventProducer.java)
4. [NotifyServiceImpl.java](/C:/JoinUp/joinup-notify/src/main/java/com/joinup/notify/service/impl/NotifyServiceImpl.java)
5. [NotifyKafkaConstants.java](/C:/JoinUp/joinup-common/src/main/java/com/joinup/common/kafka/NotifyKafkaConstants.java)

### 你读这段时应该抓什么
你要搞明白的是：
1. 通知不是一个接口，而是一条异步链路
2. 业务模块只负责发业务事件
3. 通知模块负责把事件翻译成用户能收到的消息

---

## 第11段：管理后台模块 `joinup-admin`

### 这一段干了什么
第11段完成了后台治理骨架：
1. 活动审核接口复用说明
2. 举报分页与处理
3. 用户信用人工干预统一入口
4. 风险用户监控
5. 热门活动监控
6. 操作日志分页查询
7. RBAC 权限编码预留

### 解决了什么问题
任何线下活动平台，一旦开始运营，就必须有后台治理能力。

这段解决的是：
1. 活动怎么审核
2. 举报怎么处理
3. 异常活动怎么下架
4. 风险用户怎么被运营同学看见
5. 后台人工操作怎么审计

### 用了什么技术或设计
1. 后台专用 Controller / Service / Mapper
2. `AdminPermissionChecker`
3. `AdminPermissionConstants`
4. `operation_log`
5. 后台监控查询 Mapper

### 为什么这里没有直接上完整 RBAC
因为当前项目的重点还是业务闭环先走通。

所以第11段做的是：
1. 先把后台模块边界搭起来
2. 先把权限编码约定好
3. 先用临时规则 `username=admin` 兜住
4. 后面再把正式 RBAC 接进来

这是一种很务实的推进方式。

### 关键代码
1. [AdminReportServiceImpl.java](/C:/JoinUp/joinup-admin/src/main/java/com/joinup/admin/service/impl/AdminReportServiceImpl.java)
2. [AdminUserServiceImpl.java](/C:/JoinUp/joinup-admin/src/main/java/com/joinup/admin/service/impl/AdminUserServiceImpl.java)
3. [AdminMonitorServiceImpl.java](/C:/JoinUp/joinup-admin/src/main/java/com/joinup/admin/service/impl/AdminMonitorServiceImpl.java)
4. [OperationLogServiceImpl.java](/C:/JoinUp/joinup-admin/src/main/java/com/joinup/admin/service/impl/OperationLogServiceImpl.java)
5. [AdminPermissionChecker.java](/C:/JoinUp/joinup-admin/src/main/java/com/joinup/admin/domain/AdminPermissionChecker.java)

### 你读这段时应该抓什么
你要理解后台模块不是“额外页面接口”，而是平台进入运营阶段后的治理能力集合。

它和前台业务最大的不同是：
1. 更强调权限
2. 更强调审计
3. 更强调监控和人工干预

---

## 第12段：项目收口与迭代底稿

### 这一段干了什么
第12段做的不是新业务，而是工程化收口：
1. Redis Key 汇总
2. Kafka 事件链路汇总
3. 报名、候补、结算时序图
4. 单元测试建议
5. 集成测试建议
6. 接口文档组织建议
7. README 初稿
8. 开发顺序建议
9. 后续扩展方向建议

### 解决了什么问题
很多项目写到这里就停了，只剩代码，没有工程总结。

第12段解决的是：
1. 后面接手的人怎么快速建立全局认知
2. 后续继续迭代时优先做什么
3. 哪些已经落地，哪些只是预留点
4. 这个项目如何从“能跑起来”走向“能持续演进”

### 用了什么方式
1. 文档收口
2. 公共 Redis Key 入口类
3. Mermaid 时序图
4. 分层测试建议
5. 后续模块扩展建议

### 为什么这一段很重要
因为一个真正能继续演进的项目，不只是代码本身，还包括：
1. 约定是否清楚
2. 链路是否可讲清楚
3. 测试是否知道该怎么补
4. 后续扩展是否有方向

第12段把这些都补上了。

### 关键文件
1. [phase-12-project-wrap-up.md](/C:/JoinUp/docs/phase-12-project-wrap-up.md)
2. [JoinUpRedisKeys.java](/C:/JoinUp/joinup-common/src/main/java/com/joinup/common/constants/JoinUpRedisKeys.java)
3. [README.md](/C:/JoinUp/README.md)

### 你读这段时应该抓什么
这段最重要的不是“背住所有文档”，而是知道：
1. 项目当前已经做到哪里
2. 还缺哪些关键接入
3. 后续最值得投入的三件事是什么

---

## 现在你应该怎么吃透这个项目

### 第一步：先把全链路串起来
你先按这个顺序把代码过一遍：
1. [UserController.java](/C:/JoinUp/joinup-user/src/main/java/com/joinup/user/controller/UserController.java)
2. [ActivityController.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/controller/ActivityController.java)
3. [SignupServiceImpl.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/service/impl/SignupServiceImpl.java)
4. [SignupActivityCacheService.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/domain/SignupActivityCacheService.java)
5. [SignupCommandHandler.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/domain/SignupCommandHandler.java)
6. [WaitlistPromotionDomainService.java](/C:/JoinUp/joinup-waitlist/src/main/java/com/joinup/waitlist/domain/WaitlistPromotionDomainService.java)
7. [WaitlistPromotionSignupCoordinator.java](/C:/JoinUp/joinup-signup/src/main/java/com/joinup/signup/domain/WaitlistPromotionSignupCoordinator.java)
8. [ActivitySettlementServiceImpl.java](/C:/JoinUp/joinup-activity/src/main/java/com/joinup/activity/service/impl/ActivitySettlementServiceImpl.java)
9. [CreditServiceImpl.java](/C:/JoinUp/joinup-credit/src/main/java/com/joinup/credit/service/impl/CreditServiceImpl.java)
10. [NotifyServiceImpl.java](/C:/JoinUp/joinup-notify/src/main/java/com/joinup/notify/service/impl/NotifyServiceImpl.java)
11. [AdminReportServiceImpl.java](/C:/JoinUp/joinup-admin/src/main/java/com/joinup/admin/service/impl/AdminReportServiceImpl.java)

如果这 11 个入口你都能讲清楚，这个项目你就已经真正入门了。

### 第二步：按“问题”而不是“模块”去理解
建议你用下面这些问题来反推代码：
1. 用户点报名时，为什么不会超卖
2. 同一个用户重复点报名时，为什么不会多出两条记录
3. 正式用户取消后，候补用户为什么能自动补位
4. 候补用户超时后，为什么能顺延下一位
5. 到报名截止后，为什么活动能自动成团或流局
6. 用户爽约后，信用分是在哪里被扣的
7. 一条业务状态变化，最终为什么能变成用户收到的通知

只要你能把这 7 个问题回答顺，项目就算吃透了。

### 第三步：最后再看基础设施与数据库
这一步建议再回头看：
1. [joinup_schema_v1.sql](/C:/JoinUp/joinup-infra/src/main/resources/sql/joinup_schema_v1.sql)
2. [application.yml](/C:/JoinUp/joinup-boot/src/main/resources/application.yml)
3. [SecurityConfig.java](/C:/JoinUp/joinup-infra/src/main/java/com/joinup/infrastructure/security/SecurityConfig.java)
4. [OpenApiConfig.java](/C:/JoinUp/joinup-infra/src/main/java/com/joinup/infrastructure/openapi/OpenApiConfig.java)
5. [MybatisPlusConfiguration.java](/C:/JoinUp/joinup-infra/src/main/java/com/joinup/infrastructure/config/MybatisPlusConfiguration.java)

因为到这个时候你已经知道这些配置是为谁服务的，再看就不会觉得抽象。

---

## 我建议你下一步怎么学
最自然的三步是：

1. 先让我给你出一份“报名链路精读版”
   从接口进来到 Redis、Kafka、MySQL、补偿、结果查询全部拆开讲

2. 再让我给你出一份“候补 + 成团流局精读版”
   把活动生命周期后半段完全讲透

3. 最后做一份“信用 + 通知 + 后台治理精读版”
   把平台治理和运营能力补齐理解

这样你不是泛泛地看代码，而是按最关键的业务链路逐条吃透。