package com.joinup.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 当前用户资料修改请求。
 */
@Data
public class UserProfileUpdateRequest {

    /** 昵称为必填，避免资料页出现空展示名。 */
    @NotBlank
    @Size(max = 64)
    private String nickname;

    /** 允许前端按需更新手机号。 */
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    /** 允许前端按需更新邮箱。 */
    @Email
    @Size(max = 128)
    private String email;

    @Size(max = 512)
    private String avatarUrl;

    /** 取值见 GenderEnum。 */
    @Min(0)
    @Max(3)
    private Integer gender;

    private LocalDate birthday;

    @Size(max = 128)
    private String schoolName;

    @Size(max = 128)
    private String major;

    @Size(max = 500)
    private String bio;

    @Size(max = 64)
    private String city;
}
