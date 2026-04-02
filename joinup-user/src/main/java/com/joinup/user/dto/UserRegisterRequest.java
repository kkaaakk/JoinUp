package com.joinup.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {

    @NotBlank
    @Size(min = 4, max = 64)
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    @NotBlank
    @Size(min = 6, max = 64)
    private String confirmPassword;

    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @Email
    @Size(max = 128)
    private String email;

    @Size(max = 64)
    private String nickname;
}
