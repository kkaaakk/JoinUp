package com.joinup.waitlist.service;

import com.joinup.common.context.LoginUser;
import com.joinup.waitlist.vo.WaitlistActivityVO;
import com.joinup.waitlist.vo.WaitlistMyItemVO;

import java.util.List;

/**
 * 候补查询服务接口。
 */
public interface WaitlistService {

    /**
     * 查询某个活动当前的候补队列信息。
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 候补队列视图对象
     */
    WaitlistActivityVO getActivityWaitlist(Long activityId, LoginUser loginUser);

    /**
     * 查询当前用户的候补记录列表。
     *
     * @param loginUser 当前登录用户
     * @return 候补记录列表
     */
    List<WaitlistMyItemVO> listMyWaitlists(LoginUser loginUser);
}
