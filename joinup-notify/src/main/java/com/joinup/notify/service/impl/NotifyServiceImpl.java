package com.joinup.notify.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.notify.domain.NotifyChannelSender;
import com.joinup.notify.domain.NotifySendExecutionResult;
import com.joinup.notify.dto.NotifyPageQuery;
import com.joinup.notify.entity.NotifyMessageEntity;
import com.joinup.notify.enums.NotifyChannelEnum;
import com.joinup.notify.enums.NotifyMessageTypeEnum;
import com.joinup.notify.enums.NotifyStatusEnum;
import com.joinup.notify.mapper.NotifyMessageMapper;
import com.joinup.notify.service.NotifyService;
import com.joinup.notify.vo.NotifyMessageItemVO;
import com.joinup.notify.vo.NotifyPageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知模块应用服务实现。
 * <p>
 * 这一层同时承担两类职责：
 * </p>
 * <p>
 * 1. 提供用户侧的通知分页和已读能力。
 * </p>
 * <p>
 * 2. 承接 `notify-send` 命令，把通知真正分发到各个渠道，并记录发送结果。
 * </p>
 */
@Service
@Validated
public class NotifyServiceImpl implements NotifyService {

    private final NotifyMessageMapper notifyMessageMapper;
    private final List<NotifyChannelSender> channelSenders;

    /**
     * 构造通知应用服务。
     *
     * @param notifyMessageMapper 通知消息 Mapper
     * @param channelSenders 渠道发送器列表
     */
    public NotifyServiceImpl(NotifyMessageMapper notifyMessageMapper,
                             List<NotifyChannelSender> channelSenders) {
        this.notifyMessageMapper = notifyMessageMapper;
        this.channelSenders = channelSenders;
    }

    /**
     * 分页查询当前用户的站内通知。
     *
     * @param currentUserId 当前用户 ID
     * @param query 分页查询条件
     * @return 分页结果
     */
    @Override
    public NotifyPageVO pageCurrentUserNotifications(Long currentUserId, NotifyPageQuery query) {
        NotifyPageQuery safeQuery = query == null ? new NotifyPageQuery() : query;
        Page<NotifyMessageEntity> page = new Page<>(safeQuery.getPageNum(), safeQuery.getPageSize());
        Page<NotifyMessageEntity> result = notifyMessageMapper.selectPage(page,
                Wrappers.<NotifyMessageEntity>lambdaQuery()
                        .eq(NotifyMessageEntity::getUserId, currentUserId)
                        .eq(NotifyMessageEntity::getChannel, NotifyChannelEnum.IN_APP.getCode())
                        .eq(safeQuery.getStatus() != null, NotifyMessageEntity::getStatus, safeQuery.getStatus())
                        .eq(safeQuery.getMessageType() != null, NotifyMessageEntity::getMessageType, safeQuery.getMessageType())
                        .isNull(Boolean.TRUE.equals(safeQuery.getUnreadOnly()), NotifyMessageEntity::getReadTime)
                        .orderByDesc(NotifyMessageEntity::getSendTime)
                        .orderByDesc(NotifyMessageEntity::getCreatedAt));

        return NotifyPageVO.builder()
                .total(result.getTotal())
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .records(result.getRecords().stream().map(this::toItemVO).toList())
                .build();
    }

    /**
     * 把一条通知标记为已读。
     *
     * @param currentUserId 当前用户 ID
     * @param messageId 通知 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long currentUserId, Long messageId) {
        NotifyMessageEntity entity = notifyMessageMapper.selectOne(Wrappers.<NotifyMessageEntity>lambdaQuery()
                .eq(NotifyMessageEntity::getId, messageId)
                .eq(NotifyMessageEntity::getUserId, currentUserId)
                .eq(NotifyMessageEntity::getChannel, NotifyChannelEnum.IN_APP.getCode())
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOTIFY_MESSAGE_NOT_FOUND);
        }
        if (NotifyStatusEnum.READ.getCode() == entity.getStatus()) {
            return;
        }
        entity.setStatus(NotifyStatusEnum.READ.getCode());
        entity.setReadTime(LocalDateTime.now());
        notifyMessageMapper.updateById(entity);
    }

    /**
     * 执行一次通知发送命令。
     *
     * @param event 通知发送命令事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(NotifySendEvent event) {
        List<Integer> channelCodes = (event.getChannels() == null || event.getChannels().isEmpty())
                ? List.of(NotifyChannelEnum.IN_APP.getCode())
                : event.getChannels();
        for (Integer channelCode : channelCodes) {
            NotifyChannelEnum channel = NotifyChannelEnum.fromCode(channelCode);
            if (channel == null) {
                throw new BusinessException(ErrorCode.NOTIFY_CHANNEL_NOT_SUPPORTED, "未知通知渠道：" + channelCode);
            }
            NotifyChannelSender sender = findSender(channel);
            NotifySendExecutionResult result = sender.send(event, channel);
            saveNotifyMessage(event, channel, result);
        }
    }

    /**
     * 根据渠道查找具体发送器。
     *
     * @param channel 通知渠道
     * @return 渠道发送器
     */
    private NotifyChannelSender findSender(NotifyChannelEnum channel) {
        return channelSenders.stream()
                .filter(sender -> sender.supports(channel))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFY_CHANNEL_NOT_SUPPORTED,
                        "未找到渠道发送器：" + channel.getDescription()));
    }

    /**
     * 把发送结果持久化到通知表。
     *
     * @param event 通知发送命令事件
     * @param channel 发送渠道
     * @param result 发送结果
     */
    private void saveNotifyMessage(NotifySendEvent event,
                                   NotifyChannelEnum channel,
                                   NotifySendExecutionResult result) {
        NotifyMessageEntity entity = new NotifyMessageEntity();
        entity.setUserId(event.getUserId());
        entity.setMessageType(event.getMessageType());
        entity.setChannel(channel.getCode());
        entity.setTitle(event.getTitle());
        entity.setContent(event.getContent());
        entity.setBizType(event.getBizType());
        entity.setBizId(event.getBizId());
        entity.setStatus(result.getStatus().getCode());
        entity.setSendTime(result.getSendTime());
        entity.setFailReason(result.getFailReason());
        notifyMessageMapper.insert(entity);
    }

    /**
     * 把通知实体转换成分页展示对象。
     *
     * @param entity 通知实体
     * @return 展示对象
     */
    private NotifyMessageItemVO toItemVO(NotifyMessageEntity entity) {
        NotifyMessageTypeEnum messageType = NotifyMessageTypeEnum.fromCode(entity.getMessageType());
        NotifyChannelEnum channel = NotifyChannelEnum.fromCode(entity.getChannel());
        NotifyStatusEnum status = NotifyStatusEnum.fromCode(entity.getStatus());
        return NotifyMessageItemVO.builder()
                .id(entity.getId())
                .messageType(entity.getMessageType())
                .messageTypeDescription(messageType != null ? messageType.getDescription() : null)
                .channel(entity.getChannel())
                .channelDescription(channel != null ? channel.getDescription() : null)
                .title(entity.getTitle())
                .content(entity.getContent())
                .bizType(entity.getBizType())
                .bizId(entity.getBizId())
                .status(entity.getStatus())
                .statusDescription(status != null ? status.getDescription() : null)
                .sendTime(entity.getSendTime())
                .readTime(entity.getReadTime())
                .build();
    }
}