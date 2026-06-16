package com.minimax.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.dto.LoginRequest;
import com.minimax.auth.dto.RegisterRequest;
import com.minimax.auth.entity.AuthLoginLog;
import com.minimax.auth.entity.AuthRefreshToken;
import com.minimax.auth.entity.SysUser;
import com.minimax.auth.entity.SysUserRole;
import com.minimax.auth.jwt.JwtProperties;
import com.minimax.auth.jwt.JwtTokenProvider;
import com.minimax.auth.mapper.AuthLoginLogMapper;
import com.minimax.auth.mapper.AuthRefreshTokenMapper;
import com.minimax.auth.mapper.SysUserMapper;
import com.minimax.auth.mapper.SysUserRoleMapper;
import com.minimax.auth.service.AuthService;
import com.minimax.auth.vo.LoginResponse;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import com.minimax.common.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final AuthRefreshTokenMapper refreshMapper;
    private final AuthLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProps;

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest req, HttpServletRequest http) {
        SysUser exist = userMapper.selectByUsername(req.getUsername());
        if (exist != null) {
            throw new BizException(ResultCode.USER_EXISTS);
        }
        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        u.setEmail(req.getEmail());
        u.setStatus(1);
        u.setTenantId(0L);
        userMapper.insert(u);

        // 默认给 USER 角色
        SysUserRole ur = new SysUserRole();
        ur.setUserId(u.getId());
        ur.setRoleId(2L); // 假定 USER 角色 id=2（与初始化 SQL 对齐）
        userRoleMapper.insert(ur);

        recordLog(u.getId(), u.getUsername(), http, 1, "注册成功");
        return buildLoginResponse(u);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest req, HttpServletRequest http) {
        SysUser u = userMapper.selectByUsername(req.getUsername());
        if (u == null) {
            recordLog(null, req.getUsername(), http, 0, "用户不存在");
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }
        if (u.getStatus() == null || u.getStatus() == 0) {
            recordLog(u.getId(), u.getUsername(), http, 0, "账号已禁用");
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            recordLog(u.getId(), u.getUsername(), http, 0, "密码错误");
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }

        u.setLastLoginIp(IpUtils.getClientIp(http));
        u.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(u);

        recordLog(u.getId(), u.getUsername(), http, 1, "登录成功");
        return buildLoginResponse(u);
    }

    @Override
    @Transactional
    public LoginResponse refresh(String refreshTokenRaw, HttpServletRequest http) {
        String hash = jwt.hashRefreshToken(refreshTokenRaw);
        AuthRefreshToken row = refreshMapper.selectOne(
                new LambdaQueryWrapper<AuthRefreshToken>().eq(AuthRefreshToken::getToken, hash));
        if (row == null || row.getRevoked() == 1 || row.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.USER_TOKEN_EXPIRED, "刷新令牌已失效，请重新登录");
        }
        SysUser u = userMapper.selectById(row.getUserId());
        if (u == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        // 旧的 revoke，签发新的
        refreshMapper.revokeByToken(hash);
        recordLog(u.getId(), u.getUsername(), http, 1, "刷新令牌");
        return buildLoginResponse(u);
    }

    @Override
    public void logout(Long userId, String refreshTokenRaw) {
        if (refreshTokenRaw != null && !refreshTokenRaw.isBlank()) {
            refreshMapper.revokeByToken(jwt.hashRefreshToken(refreshTokenRaw));
        } else if (userId != null) {
            refreshMapper.revokeByUserId(userId);
        }
    }

    @Override
    public LoginResponse.UserInfo me(Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) throw new BizException(ResultCode.USER_NOT_FOUND);
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        return LoginResponse.UserInfo.builder()
                .id(u.getId()).username(u.getUsername()).nickname(u.getNickname())
                .email(u.getEmail()).avatar(u.getAvatar()).roles(roles)
                .superAdmin(roles != null && roles.contains("SUPER_ADMIN"))
                .build();
    }

    // ---------------------- private helpers ----------------------

    private LoginResponse buildLoginResponse(SysUser u) {
        List<String> roles = userMapper.selectRoleCodesByUserId(u.getId());
        boolean superAdmin = roles != null && roles.contains("SUPER_ADMIN");

        String access = jwt.issueAccessToken(u.getId(), u.getUsername(), roles);
        String refresh = jwt.issueRefreshToken();

        AuthRefreshToken row = new AuthRefreshToken();
        row.setUserId(u.getId());
        row.setToken(jwt.hashRefreshToken(refresh));
        row.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProps.getRefreshTtlSeconds()));
        row.setRevoked(0);
        refreshMapper.insert(row);

        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(jwtProps.getAccessTtlSeconds())
                .tokenType("Bearer")
                .user(LoginResponse.UserInfo.builder()
                        .id(u.getId()).username(u.getUsername()).nickname(u.getNickname())
                        .email(u.getEmail()).avatar(u.getAvatar()).roles(roles)
                        .superAdmin(superAdmin).build())
                .build();
    }

    private void recordLog(Long userId, String username, HttpServletRequest http, int status, String msg) {
        try {
            AuthLoginLog logRow = new AuthLoginLog();
            logRow.setUserId(userId);
            logRow.setUsername(username);
            logRow.setIp(IpUtils.getClientIp(http));
            logRow.setUserAgent(http != null ? safeUa(http) : null);
            logRow.setStatus(status);
            logRow.setMessage(msg);
            loginLogMapper.insert(logRow);
        } catch (Exception e) {
            // 记日志失败不应阻塞主流程
            log.warn("写登录日志失败: {}", e.getMessage());
        }
    }

    private String safeUa(HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        if (ua == null) return null;
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}
