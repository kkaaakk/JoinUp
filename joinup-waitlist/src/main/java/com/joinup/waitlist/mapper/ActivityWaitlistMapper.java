package com.joinup.waitlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joinup.waitlist.entity.ActivityWaitlistEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 候补表访问器。
 * <p>
 * 这里保留了候补模块最常用的几类查询：按活动取队列、按用户查记录、查待超时确认数据。
 * </p>
 */
@Mapper
public interface ActivityWaitlistMapper extends BaseMapper<ActivityWaitlistEntity> {

    /**
     * 查询某个活动当前最大的候补顺序号。
     *
     * @param activityId 活动 ID
     * @return 当前最大顺序号；如果没有候补记录则返回 0
     */
    @Select("""
            SELECT COALESCE(MAX(queue_no), 0)
            FROM activity_waitlist
            WHERE activity_id = #{activityId}
              AND deleted = 0
            """)
    Integer selectMaxQueueNo(@Param("activityId") Long activityId);

    /**
     * 根据活动 ID 和用户 ID 查询候补记录。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 候补记录；不存在时返回 {@code null}
     */
    @Select("""
            SELECT *
            FROM activity_waitlist
            WHERE activity_id = #{activityId}
              AND user_id = #{userId}
              AND deleted = 0
            LIMIT 1
            """)
    ActivityWaitlistEntity selectByActivityIdAndUserId(@Param("activityId") Long activityId,
                                                       @Param("userId") Long userId);

    /**
     * 查询某个活动队头的一批候补记录。
     *
     * @param activityId 活动 ID
     * @param status 候补状态
     * @param limit 查询条数
     * @return 候补记录列表
     */
    @Select("""
            SELECT *
            FROM activity_waitlist
            WHERE activity_id = #{activityId}
              AND status = #{status}
              AND deleted = 0
            ORDER BY queue_no ASC
            LIMIT #{limit}
            """)
    List<ActivityWaitlistEntity> selectTopByActivityIdAndStatus(@Param("activityId") Long activityId,
                                                                @Param("status") Integer status,
                                                                @Param("limit") Integer limit);

    /**
     * 查询已经超过确认截止时间、需要顺延处理的候补记录。
     *
     * @param status 候补状态
     * @param deadline 截止时间
     * @param limit 查询条数
     * @return 超时待处理候补记录列表
     */
    @Select("""
            SELECT *
            FROM activity_waitlist
            WHERE status = #{status}
              AND confirm_deadline IS NOT NULL
              AND confirm_deadline <= #{deadline}
              AND deleted = 0
            ORDER BY confirm_deadline ASC
            LIMIT #{limit}
            """)
    List<ActivityWaitlistEntity> selectExpiredWaitingConfirm(@Param("status") Integer status,
                                                             @Param("deadline") LocalDateTime deadline,
                                                             @Param("limit") Integer limit);
}
