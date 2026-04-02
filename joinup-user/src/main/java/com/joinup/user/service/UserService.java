package com.joinup.user.service;

import com.joinup.user.dto.UserLoginRequest;
import com.joinup.user.dto.UserProfileUpdateRequest;
import com.joinup.user.dto.UserRegisterRequest;
import com.joinup.user.vo.UserCreditVO;
import com.joinup.user.vo.UserLoginVO;
import com.joinup.user.vo.UserProfileVO;
import com.joinup.user.vo.UserRegisterVO;

public interface UserService {

    UserRegisterVO register(UserRegisterRequest request);

    UserLoginVO login(UserLoginRequest request);

    UserProfileVO getCurrentUserProfile(Long currentUserId);

    UserProfileVO updateCurrentUserProfile(Long currentUserId, UserProfileUpdateRequest request);

    UserCreditVO getCurrentUserCredit(Long currentUserId);

    void updateUserStatus(Long userId, Integer status);
}
