package com.joinup.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joinup.activity.entity.ActivityReportEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理后台举报数据访问器。
 * <p>
 * 这里直接复用 activity 模块里已经定义好的举报实体，避免在 admin 模块重复维护一套表映射。
 * </p>
 */
@Mapper
public interface AdminActivityReportMapper extends BaseMapper<ActivityReportEntity> {
}