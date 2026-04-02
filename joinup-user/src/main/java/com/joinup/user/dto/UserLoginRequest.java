package com.joinup.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录请求。
 */
@Data
public class UserLoginRequest {

    /** 支持用户名、手机号或邮箱任一标识登录。 */
    @NotBlank
    @Size(max = 128)
    private String identifier;

    /** 明文密码，仅用于本次认证比对。 */
    @NotBlank
    @Size(min = 6, max = 64)
    private String password;
}
