package com.joinup.admin.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理后台监控查询数据访问器。
 * <p>
 * 这里承接的是“偏运营、偏监控”的查询，和业务模块里的读写职责做一个轻度分离。
 * 这样后续如果要把后台监控查询迁到独立读库，改动面会更小。
 * </p>
 */
@Mapper
public interface AdminMonitorMapper {

    /**
     * 分页查询热门活动。
     * <p>
     * 当前按热度分、浏览量、当前人数倒序排序，先满足运营后台“快速看榜单”的需求。
     * 后续如果要加入报名转化率、举报率等复杂指标，可以继续在这里扩展 SQL。
     * </p>
     *
     * @param page 分页对象
     * @param minHeatScore 最低热度分筛选
     * @return 热门活动分页结果
     */
    @Select("""
            <script>
            SELECT *
            FROM activity
            WHERE deleted = 0
              AND status IN (20, 30, 40, 50, 60, 80)
            <if test="minHeatScore != null">
              AND heat_score <![CDATA[>=]]> #{minHeatScore}
            </if>
            ORDER BY heat_score DESC, view_count DESC, current_participants DESC, id DESC
            </script>
            """)
    Page<ActivityEntity> selectHotActivityPage(Page<ActivityEntity> page,
                                               @Param("minHeatScore") Integer minHeatScore);

    /**
     * 分页查询风险用户。
     * <p>
     * 风险用户主要包含两类：
     * 1. 信用分已经低于风险阈值的用户；
     * 2. 状态异常的用户，例如已禁用用户。
     * </p>
     *
     * @param page 分页对象
     * @param maxCreditScore 风险信用分阈值
     * @param includeDisabled 是否把已禁用用户也纳入结果
     * @return 风险用户分页结果
     */
    @Select("""
            <script>
            SELECT *
            FROM `user`
            WHERE deleted = 0
              AND (
                    credit_score <![CDATA[<=]]> #{maxCreditScore}
                    <if test="includeDisabled != null and includeDisabled">
                      OR status <![CDATA[<>]]> 1
                    </if>
                  )
            ORDER BY credit_score ASC, status DESC, updated_at DESC, id DESC
            </script>
            """)
    Page<UserEntity> selectRiskUserPage(Page<UserEntity> page,
                                        @Param("maxCreditScore") Integer maxCreditScore,
                                        @Param("includeDisabled") Boolean includeDisabled);
}