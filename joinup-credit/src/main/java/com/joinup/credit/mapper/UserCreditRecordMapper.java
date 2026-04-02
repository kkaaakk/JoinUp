package com.joinup.credit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joinup.credit.entity.UserCreditRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信用记录 Mapper。
 * <p>
 * 当前阶段主要复用 MyBatis Plus 的通用能力，复杂查询先收敛在 service 里用 Wrapper 组合。
 * 如果后面出现更重的风控统计 SQL，再把专项查询方法下沉到这里即可。
 * </p>
 */
@Mapper
public interface UserCreditRecordMapper extends BaseMapper<UserCreditRecordEntity> {
}