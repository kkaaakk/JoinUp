package com.joinup.signup.controller;

import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import com.joinup.signup.service.SignupService;
import com.joinup.signup.vo.SignupActivityResultVO;
import com.joinup.signup.vo.SignupApplyVO;
import com.joinup.signup.vo.SignupMyItemVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报名模块对外 HTTP 控制器。
 * <p>
 * 该控制器只承担接口入参接收、登录用户注入、返回结构封装等职责，
 * 不直接处理 Redis 抢位、Kafka 投递或数据库落库等复杂业务逻辑。
 * </p>
 */
@Validated
@RestController
@RequestMapping("/api/signup")
public class SignupController {

    private final SignupService signupService;

    /**
     * 构造报名控制器。
     *
     * @param signupService 报名应用服务
     */
    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    /**
     * 发起报名申请。
     * <p>
     * 接口收到请求后，会进入报名模块的高并发主链路：
     * 先在 Redis 中原子预占席位，再异步投递 Kafka 命令，最后由消费者写库。
     * 因此当前接口返回的通常是“已受理 / 处理中”结果，而不是同步写库后的最终状态。
     * </p>
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 报名受理结果
     */
    @PostMapping("/apply/{activityId}")
    public Result<SignupApplyVO> apply(@PathVariable("activityId") Long activityId,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(signupService.apply(activityId, loginUser));
    }

    /**
     * 发起取消报名请求。
     * <p>
     * 当前取消操作也走异步链路，避免在同步请求阶段直接更新数据库造成热点竞争。
     * </p>
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @param reason 取消原因，可为空
     * @return 取消受理结果
     */
    @PostMapping("/cancel/{activityId}")
    public Result<SignupApplyVO> cancel(@PathVariable("activityId") Long activityId,
                                        @AuthenticationPrincipal LoginUser loginUser,
                                        @RequestParam(value = "reason", required = false) String reason) {
        return Result.success(signupService.cancel(activityId, loginUser, reason));
    }

    /**
     * 查询当前用户最近的报名记录。
     * <p>
     * 该接口会把正式报名记录和候补记录统一聚合后返回，方便前端直接展示“我的报名”列表。
     * </p>
     *
     * @param loginUser 当前登录用户
     * @return 当前用户的报名记录列表
     */
    @GetMapping("/my")
    public Result<List<SignupMyItemVO>> my(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(signupService.listMySignups(loginUser));
    }

    /**
     * 查询当前用户在指定活动上的报名结果。
     * <p>
     * 如果异步写库尚未完成，该接口会优先返回 Redis 中的处理中状态，
     * 让前端能够及时感知报名命令已经进入处理链路。
     * </p>
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 当前用户在该活动上的报名结果
     */
    @GetMapping("/activity/{activityId}")
    public Result<SignupActivityResultVO> activity(@PathVariable("activityId") Long activityId,
                                                   @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(signupService.getCurrentUserActivityResult(activityId, loginUser));
    }
}
