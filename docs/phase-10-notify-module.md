# 第10段：通知模块与 Kafka 事件驱动设计

## 1. 通知场景覆盖

本段在 `joinup-notify` 中落地了以下场景的统一通知能力：

1. 报名成功通知
2. 候补成功通知
3. 成团通知
4. 流局通知
5. 即将开始提醒（通过 `notify-send` 直接下发预留）
6. 活动取消通知（通过 `signup-canceled` 的 `activityCanceled=true` 或直接发送 `notify-send` 预留）
7. 信用变更通知

## 2. Kafka Topic 设计

跨模块共用的 topic 常量统一放在 `joinup-common`：

- `signup-created`
- `signup-canceled`
- `waitlist-promoted`
- `activity-group-success`
- `activity-group-failed`
- `notify-send`
- `credit-changed`

说明：

1. 前 6 个是业务事件主题，通知模块负责消费
2. `notify-send` 是通知模块内部统一发送命令主题
3. 上游模块先发布业务事件，通知模块再转成 `NotifySendEvent`，从而解耦通知文案与业务主流程

## 3. notify_message 表对应代码

对应代码：

- `NotifyMessageEntity`
- `NotifyMessageMapper`
- `NotifyStatusEnum`
- `NotifyChannelEnum`
- `NotifyMessageTypeEnum`

其中：

1. `channel` 用于区分站内信、短信、邮件、小程序
2. `status` 用于追踪待发送、已发送、发送失败、已读
3. 当前用户分页接口只读取 `IN_APP` 渠道记录

## 4. 事件生产与消费链路

### 上游业务事件 -> 通知发送命令

由 `NotifyBusinessEventConsumer` 消费：

- `signup-created`
- `signup-canceled`
- `waitlist-promoted`
- `activity-group-success`
- `activity-group-failed`
- `credit-changed`

消费后通过 `NotifyMessageFactory` 生成 `NotifySendEvent`，再由 `NotifySendEventProducer` 投递到 `notify-send`。

### 通知发送命令 -> 渠道分发

由 `NotifySendConsumer` 消费 `notify-send`，调用 `NotifyService.dispatch(...)`：

1. 选择具体渠道发送器
2. 记录发送状态
3. 站内信直接落 `notify_message`
4. 短信 / 邮件 / 小程序当前保留接口和失败占位结果

## 5. 站内信落库逻辑

站内信由 `InAppNotifyChannelSender` 返回成功结果，再由 `NotifyServiceImpl.saveNotifyMessage(...)` 统一落表。

这样做的好处：

1. 渠道发送逻辑和持久化逻辑分离
2. 后续外部渠道也能复用同一套发送结果记录逻辑
3. 分页接口可以直接读取同一张通知表

## 6. 分页与已读接口

### 用户接口

- `GET /api/notify/page`
- `POST /api/notify/read/{id}`

说明：

1. 分页接口支持 `pageNum / pageSize / status / messageType / unreadOnly`
2. 已读接口只允许当前用户把自己的站内信标记为已读

## 7. 后续扩展预留

已预留以下扩展点：

- `NotifyChannelSender`
- `SmsNotifyChannelSender`
- `EmailNotifyChannelSender`
- `MiniProgramNotifyChannelSender`

当前这些外部渠道先返回占位失败结果，不影响站内信主链路。