package com.minimax.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "用户名仅支持字母/数字/._-")
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    @Size(max = 64)
    private String nickname;

    @Email
    private String email;
}
