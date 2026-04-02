package com.joinup.user.service;

import com.joinup.user.dto.UserLoginRequest;
import com.joinup.user.dto.UserProfileUpdateRequest;
import com.joinup.user.dto.UserRegisterRequest;
import com.joinup.user.vo.UserCreditVO;
import com.joinup.user.vo.UserLoginVO;
import com.joinup.user.vo.UserProfileVO;
import com.joinup.user.vo.UserRegisterVO;

public interface UserService {

    /** 注册新用户并初始化资料。 */
    UserRegisterVO register(UserRegisterRequest request);

    /** 校验账号密码并签发 JWT。 */
    UserLoginVO login(UserLoginRequest request);

    /** 查询当前登录用户资料。 */
    UserProfileVO getCurrentUserProfile(Long currentUserId);

    /** 更新当前登录用户资料。 */
    UserProfileVO updateCurrentUserProfile(Long currentUserId, UserProfileUpdateRequest request);

    /** 查询当前用户信用分概览。 */
    UserCreditVO getCurrentUserCredit(Long currentUserId);

    /** 更新用户状态，预留给后台治理和风控。 */
    void updateUserStatus(Long userId, Integer status);
}
