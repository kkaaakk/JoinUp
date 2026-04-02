package com.joinup.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joinup.notify.entity.NotifyMessageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知消息表访问器。
 * <p>
 * 当前候补模块只需要把补位提醒写入通知表，因此先提供最小 Mapper 能力。
 * </p>
 */
@Mapper
public interface NotifyMessageMapper extends BaseMapper<NotifyMessageEntity> {
}
