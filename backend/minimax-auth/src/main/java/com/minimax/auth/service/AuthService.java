package com.minimax.auth.service;

import com.minimax.auth.dto.LoginRequest;
import com.minimax.auth.dto.RegisterRequest;
import com.minimax.auth.vo.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponse register(RegisterRequest req, HttpServletRequest http);

    LoginResponse login(LoginRequest req, HttpServletRequest http);

    LoginResponse refresh(String refreshTokenRaw, HttpServletRequest http);

    void logout(Long userId, String refreshTokenRaw);

    LoginResponse.UserInfo me(Long userId);

    /**
     * 给非 Web 场景 (微信扫码/SSO/OAuth) 生成登录响应 (不写登录日志).
     */
    LoginResponse issueLoginResponse(com.minimax.auth.entity.SysUser user, HttpServletRequest ignore);
}
