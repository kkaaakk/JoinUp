package com.joinup.common.context;

import lombok.Builder;
import lombok.Getter;

/**
 * 登录态用户快照。
 * 这里只保留认证链路需要的最小字段，避免把完整用户实体直接放进安全上下文。
 */
@Getter
@Builder
public class LoginUser {

    /** 当前登录用户 ID。 */
    private Long userId;
    /** 用户名，便于日志和权限扩展。 */
    private String username;
    /** 昵称，用于接口直接回显。 */
    private String nickname;
    /** 当前信用分，预留给报名资格校验。 */
    private Integer creditScore;
    /** 用户状态，便于过滤器或业务层快速判定可用性。 */
    private Integer status;
}
