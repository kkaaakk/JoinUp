package com.joinup.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.infrastructure.security.JwtTokenProvider;
import com.joinup.user.dto.UserLoginRequest;
import com.joinup.user.dto.UserProfileUpdateRequest;
import com.joinup.user.dto.UserRegisterRequest;
import com.joinup.user.entity.UserEntity;
import com.joinup.user.entity.UserProfileEntity;
import com.joinup.user.enums.GenderEnum;
import com.joinup.user.enums.UserStatusEnum;
import com.joinup.user.mapper.UserMapper;
import com.joinup.user.mapper.UserProfileMapper;
import com.joinup.user.service.UserService;
import com.joinup.user.vo.UserCreditVO;
import com.joinup.user.vo.UserLoginVO;
import com.joinup.user.vo.UserProfileVO;
import com.joinup.user.vo.UserRegisterVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * 用户模块应用服务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserServiceImpl(UserMapper userMapper,
                           UserProfileMapper userProfileMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterVO register(UserRegisterRequest request) {
        // 注册时先统一做字段归一化，避免空串和首尾空格绕过唯一校验。
        String username = normalizeRequired(request.getUsername());
        String phone = normalizeNullable(request.getPhone());
        String email = normalizeNullable(request.getEmail());
        String nickname = normalizeNullable(request.getNickname());

        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        validateUniqueFields(username, phone, email, null);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatusEnum.ACTIVE.getCode());
        user.setCreditScore(100);
        userMapper.insert(user);

        // 用户主表和资料表在同一事务内初始化，避免出现半成功状态。
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(user.getId());
        profile.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        profile.setGender(GenderEnum.UNKNOWN.getCode());
        userProfileMapper.insert(profile);

        return UserRegisterVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(profile.getNickname())
                .status(user.getStatus())
                .statusDescription(getUserStatusDescription(user.getStatus()))
                .creditScore(user.getCreditScore())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO login(UserLoginRequest request) {
        String identifier = normalizeRequired(request.getIdentifier());
        UserEntity user = findByIdentifier(identifier);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        assertUserAvailable(user);

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        UserProfileEntity profile = findProfileByUserId(user.getId());
        // Token 里只放当前鉴权需要的快照字段，避免泄露过多用户信息。
        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(profile != null ? profile.getNickname() : user.getUsername())
                .creditScore(user.getCreditScore())
                .status(user.getStatus())
                .build();

        return UserLoginVO.builder()
                .accessToken(jwtTokenProvider.createToken(loginUser))
                .tokenType(TOKEN_TYPE)
                .expiresInSeconds(jwtTokenProvider.getExpireSeconds())
                .profile(buildUserProfileVO(user, profile))
                .build();
    }

    @Override
    public UserProfileVO getCurrentUserProfile(Long currentUserId) {
        UserEntity user = getAvailableUser(currentUserId);
        return buildUserProfileVO(user, findProfileByUserId(currentUserId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateCurrentUserProfile(Long currentUserId, UserProfileUpdateRequest request) {
        UserEntity user = getAvailableUser(currentUserId);
        String phone = request.getPhone() == null ? user.getPhone() : normalizeNullable(request.getPhone());
        String email = request.getEmail() == null ? user.getEmail() : normalizeNullable(request.getEmail());

        // 手机号、邮箱即使是更新场景也要排除自己后再做唯一性校验。
        validateUniqueFields(null, phone, email, currentUserId);

        user.setPhone(phone);
        user.setEmail(email);
        userMapper.updateById(user);

        UserProfileEntity profile = findProfileByUserId(currentUserId);
        if (profile == null) {
            profile = new UserProfileEntity();
            profile.setUserId(currentUserId);
        }

        profile.setNickname(normalizeRequired(request.getNickname()));
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(normalizeNullable(request.getAvatarUrl()));
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        } else if (profile.getGender() == null) {
            profile.setGender(GenderEnum.UNKNOWN.getCode());
        }
        if (request.getBirthday() != null) {
            profile.setBirthday(request.getBirthday());
        }
        if (request.getSchoolName() != null) {
            profile.setSchoolName(normalizeNullable(request.getSchoolName()));
        }
        if (request.getMajor() != null) {
            profile.setMajor(normalizeNullable(request.getMajor()));
        }
        if (request.getBio() != null) {
            profile.setBio(normalizeNullable(request.getBio()));
        }
        if (request.getCity() != null) {
            profile.setCity(normalizeNullable(request.getCity()));
        }

        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }

        return buildUserProfileVO(user, profile);
    }

    @Override
    public UserCreditVO getCurrentUserCredit(Long currentUserId) {
        UserEntity user = getAvailableUser(currentUserId);
        return UserCreditVO.builder()
                .userId(user.getId())
                .creditScore(user.getCreditScore())
                .status(user.getStatus())
                .statusDescription(getUserStatusDescription(user.getStatus()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        UserEntity user = getUserOrThrow(userId);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private UserEntity getAvailableUser(Long userId) {
        UserEntity user = getUserOrThrow(userId);
        assertUserAvailable(user);
        return user;
    }

    private UserEntity getUserOrThrow(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserEntity findByIdentifier(String identifier) {
        // 登录入口支持用户名 / 手机号 / 邮箱三种标识，便于后面扩展账号体系。
        return userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, identifier)
                .or()
                .eq(UserEntity::getPhone, identifier)
                .or()
                .eq(UserEntity::getEmail, identifier)
                .last("limit 1"));
    }

    private UserProfileEntity findProfileByUserId(Long userId) {
        return userProfileMapper.selectOne(Wrappers.<UserProfileEntity>lambdaQuery()
                .eq(UserProfileEntity::getUserId, userId)
                .last("limit 1"));
    }

    private void validateUniqueFields(String username, String phone, String email, Long excludeUserId) {
        if (StringUtils.hasText(username) && existsUserField(UserEntity::getUsername, username, excludeUserId)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Username already exists");
        }
        if (StringUtils.hasText(phone) && existsUserField(UserEntity::getPhone, phone, excludeUserId)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Phone already exists");
        }
        if (StringUtils.hasText(email) && existsUserField(UserEntity::getEmail, email, excludeUserId)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Email already exists");
        }
    }

    private boolean existsUserField(com.baomidou.mybatisplus.core.toolkit.support.SFunction<UserEntity, String> field,
                                    String value,
                                    Long excludeUserId) {
        LambdaQueryWrapper<UserEntity> queryWrapper = Wrappers.<UserEntity>lambdaQuery().eq(field, value);
        if (excludeUserId != null) {
            queryWrapper.ne(UserEntity::getId, excludeUserId);
        }
        return userMapper.selectCount(queryWrapper) > 0;
    }

    private void assertUserAvailable(UserEntity user) {
        if (!Objects.equals(user.getStatus(), UserStatusEnum.ACTIVE.getCode())) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "User status does not allow this operation");
        }
    }

    private UserProfileVO buildUserProfileVO(UserEntity user, UserProfileEntity profile) {
        return UserProfileVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .statusDescription(getUserStatusDescription(user.getStatus()))
                .creditScore(user.getCreditScore())
                .lastLoginAt(user.getLastLoginAt())
                .nickname(profile != null ? profile.getNickname() : user.getUsername())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .gender(profile != null ? profile.getGender() : null)
                .genderDescription(getGenderDescription(profile != null ? profile.getGender() : null))
                .birthday(profile != null ? profile.getBirthday() : null)
                .schoolName(profile != null ? profile.getSchoolName() : null)
                .major(profile != null ? profile.getMajor() : null)
                .bio(profile != null ? profile.getBio() : null)
                .city(profile != null ? profile.getCity() : null)
                .build();
    }

    private String getUserStatusDescription(Integer status) {
        return Arrays.stream(UserStatusEnum.values())
                .filter(item -> Objects.equals(item.getCode(), status))
                .findFirst()
                .map(UserStatusEnum::getDescription)
                .orElse(null);
    }

    private String getGenderDescription(Integer gender) {
        return Arrays.stream(GenderEnum.values())
                .filter(item -> Objects.equals(item.getCode(), gender))
                .findFirst()
                .map(GenderEnum::getDescription)
                .orElse(null);
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        // 统一把空白串转换为 null，简化后续唯一索引和更新逻辑处理。
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
