package com.joinup.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserLoginRequest {

    @NotBlank
    @Size(max = 128)
    private String identifier;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;
}
