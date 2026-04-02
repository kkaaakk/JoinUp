package com.joinup.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joinup.activity.entity.ActivityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动表访问器。
 */
@Mapper
public interface ActivityMapper extends BaseMapper<ActivityEntity> {

    /**
     * 查询已经到达报名截止时间、且仍处于可结算状态的活动列表。
     *
     * @param deadline 截止时间上限，通常为当前时间
     * @param limit 查询数量上限
     * @return 待结算活动列表
     */
    @Select("""
            SELECT *
            FROM activity
            WHERE deleted = 0
              AND status IN (30, 40, 50)
              AND signup_deadline <= #{deadline}
            ORDER BY signup_deadline ASC
            LIMIT #{limit}
            """)
    List<ActivityEntity> selectDueForSettlement(@Param("deadline") LocalDateTime deadline,
                                                @Param("limit") Integer limit);
}
