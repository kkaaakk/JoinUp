# 第6段 - joinup-signup 高并发报名模块

## 1. 报名核心流程
1. 客户端调用 `POST /api/signup/apply/{activityId}`。
2. `SignupServiceImpl` 不直接写库，先进入 Redis 预约名额流程。
3. `SignupActivityCacheService` 预热活动快照：正式剩余名额、候补剩余名额、活动状态、报名截止时间、候补序号种子。
4. Lua 脚本在 Redis 内原子执行：
   - 判断用户是否已有进行中/成功中的报名状态
   - 判断活动是否允许报名、是否过截止时间
   - 优先扣减正式名额
   - 正式名额用完后按活动规则进入候补
   - 同时写入用户态和请求态，保证幂等
5. Redis 预约成功后，服务层投递 Kafka 命令，`activityId` 作为消息 key，确保同一活动按顺序消费。
6. Kafka 消费者串行处理同一活动的消息，在数据库内落 `activity_signup` / `activity_waitlist`，并更新 `activity` 的人数与状态。
7. 消费成功后刷新 Redis 用户状态为最终态；失败时根据失败类型选择补偿或重试。

## 2. Redis Key 设计
- 正式名额：`joinup:signup:activity:{activityId}:formal:remain`
- 候补名额：`joinup:signup:activity:{activityId}:waitlist:remain`
- 候补顺序号：`joinup:signup:activity:{activityId}:waitlist:queue-seq`
- 活动快照：`joinup:signup:activity:{activityId}:meta`
- 用户报名状态：`joinup:signup:activity:{activityId}:user:{userId}:state`
- 用户查询结果：`joinup:signup:user:{userId}:activity:{activityId}:result`
- 请求查询结果：`joinup:signup:request:{requestId}:result`

说明：
- `user:state` 用于幂等和防重复报名。
- `user:result` 用于 `GET /api/signup/activity/{activityId}` 的即时查询。
- `request:result` 方便后续扩展独立请求结果查询接口。

## 3. Lua 脚本设计
文件：`joinup-signup/src/main/resources/lua/signup_apply.lua`

原子能力包括：
1. 检查用户状态 key，防止重复点击重复扣减。
2. 检查活动状态与报名截止时间。
3. 优先扣减正式名额。
4. 正式名额不足时，根据候补规则扣减候补额度并分配递增 `queueNo`。
5. 一次性把 `userState / userResult / requestResult` 写成 `PROCESSING` 状态。

补偿脚本：`signup_compensate.lua`
- 发送 Kafka 失败或不可恢复消费失败时，回补正式/候补剩余名额，并清理用户进行中状态。

## 4. Kafka 异步写库设计
生产者：`SignupCommandProducer`
- Topic：`joinup.signup.command`
- Key：`activityId`
- Payload：`SignupCommandEvent`

消费者：`SignupCommandConsumer`
- 同一活动消息天然按分区顺序消费，数据库更新不再承受高并发行锁竞争。
- 事务内完成：报名表/候补表更新 + 活动人数与状态更新。

## 5. 幂等与重复报名控制
1. Redis `userState` 是第一道防线，直接挡掉重复点击。
2. Kafka 消费端通过“已存在记录 + 状态判断”处理重复消息。
3. 数据库 `activity_id + user_id` 唯一索引仍然保留，作为最终兜底约束。
4. 取消成功后会删除 `userState`，允许后续重新报名；但历史结果仍保留在 `userResult` 中短期可查。

## 6. 结果查询接口
- `GET /api/signup/activity/{activityId}`
  - 优先读 Redis 中的即时状态，能看到 `PROCESSING`
  - Redis 没命中时回退数据库
- `GET /api/signup/my`
  - 返回当前用户最近 100 条正式报名与候补记录

## 7. 补偿机制
### 已实现
1. Redis 预约成功但 Kafka 投递失败：立即执行 Lua 补偿回滚。
2. Kafka 消费遇到业务不可恢复异常：回补 Redis 并把请求结果标记为 `FAILED`。
3. Kafka 消费遇到瞬时异常（例如数据库故障）：抛异常给 Kafka 重试，保留 Redis `PROCESSING` 状态，不立即回滚。

### 下一步预留
1. 增加死信队列（DLQ）处理长时间失败的命令。
2. 增加定时对账任务，校验 Redis 剩余名额与数据库人数是否一致。
3. 接 activity 模块事件，在活动审核/取消/截止时主动失效报名缓存。
4. 接 waitlist 模块补位流程，在正式报名取消后自动拉起候补补位。

## 8. 本段落地文件
### joinup-signup
- `controller/SignupController.java`
- `service/SignupService.java`
- `service/impl/SignupServiceImpl.java`
- `domain/SignupActivityCacheService.java`
- `domain/SignupCommandHandler.java`
- `producer/SignupCommandProducer.java`
- `consumer/SignupCommandConsumer.java`
- `event/SignupCommandEvent.java`
- `mapper/ActivitySignupMapper.java`
- `support/SignupRedisKeys.java`
- `support/SignupCachedState.java`
- `support/SignupReservationResult.java`
- `support/SignupActivitySnapshot.java`
- `resources/lua/signup_apply.lua`
- `resources/lua/signup_compensate.lua`

### 关联支撑修改
- `joinup-common/src/main/java/com/joinup/common/exception/ErrorCode.java`
- `joinup-signup/pom.xml`
- `joinup-waitlist/src/main/java/com/joinup/waitlist/mapper/ActivityWaitlistMapper.java`