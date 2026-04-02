package com.joinup.waitlist.controller;

import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import com.joinup.waitlist.service.WaitlistService;
import com.joinup.waitlist.vo.WaitlistActivityVO;
import com.joinup.waitlist.vo.WaitlistMyItemVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 候补模块对外查询控制器。
 */
@Validated
@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    /**
     * 构造候补控制器。
     *
     * @param waitlistService 候补查询服务
     */
    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    /**
     * 查询某个活动当前的候补队列。
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 候补队列信息
     */
    @GetMapping("/activity/{activityId}")
    public Result<WaitlistActivityVO> activity(@PathVariable("activityId") Long activityId,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(waitlistService.getActivityWaitlist(activityId, loginUser));
    }

    /**
     * 查询当前用户的候补记录列表。
     *
     * @param loginUser 当前登录用户
     * @return 候补记录列表
     */
    @GetMapping("/my")
    public Result<List<WaitlistMyItemVO>> my(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(waitlistService.listMyWaitlists(loginUser));
    }
}
