package com.joinup.notify.controller;

import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.common.result.Result;
import com.joinup.notify.dto.NotifyPageQuery;
import com.joinup.notify.service.NotifyService;
import com.joinup.notify.vo.NotifyPageVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知模块对外接口。
 */
@Validated
@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    private final NotifyService notifyService;

    /**
     * 构造通知控制器。
     *
     * @param notifyService 通知模块应用服务
     */
    public NotifyController(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * 分页查询当前登录用户的站内通知。
     *
     * @param loginUser 当前登录用户快照
     * @param query 分页查询参数
     * @return 通知分页结果
     */
    @GetMapping("/page")
    public Result<NotifyPageVO> page(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid NotifyPageQuery query) {
        return Result.success(notifyService.pageCurrentUserNotifications(currentUserId(loginUser), query));
    }

    /**
     * 把指定通知标记为已读。
     *
     * @param loginUser 当前登录用户快照
     * @param id 通知 ID
     * @return 空成功结果
     */
    @PostMapping("/read/{id}")
    public Result<Void> read(@AuthenticationPrincipal LoginUser loginUser,
                             @PathVariable("id") Long id) {
        notifyService.markRead(currentUserId(loginUser), id);
        return Result.success();
    }

    /**
     * 从认证上下文中提取当前用户 ID。
     *
     * @param loginUser 当前登录用户快照
     * @return 当前用户 ID
     */
    private Long currentUserId(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser.getUserId();
    }
}