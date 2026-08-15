package com.minimax.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    /** access token 剩余秒数，前端可据此刷新 */
    private Long expiresIn;
    private String tokenType;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String avatar;
        private List<String> roles;

        /** ⭐ 超级管理员标识 (adminLiugl 独有) */
        @Builder.Default
        private Boolean superAdmin = false;
    }
}
