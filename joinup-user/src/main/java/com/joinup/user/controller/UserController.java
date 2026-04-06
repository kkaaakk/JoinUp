package com.joinup.user.controller;

import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.common.result.Result;
import com.joinup.user.dto.UserLoginRequest;
import com.joinup.user.dto.UserProfileUpdateRequest;
import com.joinup.user.dto.UserRegisterRequest;
import com.joinup.user.service.UserService;
import com.joinup.user.vo.UserCreditVO;
import com.joinup.user.vo.UserLoginVO;
import com.joinup.user.vo.UserProfileVO;
import com.joinup.user.vo.UserRegisterVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户模块对外接口。
 */
@Validated
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<UserRegisterVO> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        return Result.success(userService.login(request));
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> getCurrentUserProfile(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(userService.getCurrentUserProfile(currentUserId(loginUser)));
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateCurrentUserProfile(@AuthenticationPrincipal LoginUser loginUser,
                                                          @Valid @RequestBody UserProfileUpdateRequest request) {
        return Result.success(userService.updateCurrentUserProfile(currentUserId(loginUser), request));
    }

    @GetMapping("/credit")
    public Result<UserCreditVO> getCurrentUserCredit(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(userService.getCurrentUserCredit(currentUserId(loginUser)));
    }

    private Long currentUserId(LoginUser loginUser) {
        // 认证过滤器已经把 LoginUser 放进上下文，这里统一兜底判空。
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser.getUserId();
    }
}
