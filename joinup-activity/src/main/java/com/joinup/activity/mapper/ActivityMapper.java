package com.joinup.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joinup.activity.entity.ActivityEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityMapper extends BaseMapper<ActivityEntity> {
}
