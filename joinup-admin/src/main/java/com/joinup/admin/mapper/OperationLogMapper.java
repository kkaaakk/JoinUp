package com.joinup.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joinup.admin.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志数据访问器。
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}