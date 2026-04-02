package com.joinup.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joinup.notify.entity.NotifyMessageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知消息表访问器。
 * <p>
 * 当前阶段主要使用 MyBatis Plus 通用能力承接分页、按用户查询和按主键更新。
 * 如果后续通知查询出现更复杂的聚合需求，再把定制 SQL 下沉到这里。
 * </p>
 */
@Mapper
public interface NotifyMessageMapper extends BaseMapper<NotifyMessageEntity> {
}