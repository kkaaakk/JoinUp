package com.joinup.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileUpdateRequest {

    @NotBlank
    @Size(max = 64)
    private String nickname;

    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @Email
    @Size(max = 128)
    private String email;

    @Size(max = 512)
    private String avatarUrl;

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
