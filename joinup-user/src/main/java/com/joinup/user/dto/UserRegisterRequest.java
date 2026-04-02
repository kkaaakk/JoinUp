package com.joinup.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求。
 */
@Data
public class UserRegisterRequest {

    /** 平台内唯一用户名。 */
    @NotBlank
    @Size(min = 4, max = 64)
    private String username;

    /** 原始密码，进入 service 后会被加密存储。 */
    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    /** 二次确认密码，避免用户误输入。 */
    @NotBlank
    @Size(min = 6, max = 64)
    private String confirmPassword;

    /** 可选手机号，支持后续手机号登录。 */
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    /** 可选邮箱，支持后续邮箱登录。 */
    @Email
    @Size(max = 128)
    private String email;

    /** 初始昵称，未传时默认回退为用户名。 */
    @Size(max = 64)
    private String nickname;
}
