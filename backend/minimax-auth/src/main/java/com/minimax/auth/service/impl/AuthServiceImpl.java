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

/**
 * 鉴权服务实现 (V3.5.5+ 完整注释版)
 *
 * <h2>业务定位</h2>
 * 实现 {@link AuthService} 接口, 负责:
 * <ul>
 *   <li>注册: 创建新用户 + 默认角色 + 登录日志</li>
 *   <li>登录: 验证密码 + 签发 access/refresh token + 更新最后登录信息</li>
 *   <li>刷新: 撤销旧 refresh token + 签发新 access/refresh</li>
 *   <li>登出: 撤销 refresh token (单设备/全设备)</li>
 *   <li>查询: 当前用户信息 + 角色</li>
 *   <li>OAuth/扫码登录: 给非 Web 场景发登录响应 (不写日志)</li>
 * </ul>
 *
 * <h2>Token 设计</h2>
 * <ul>
 *   <li>access token: 短期 (e.g. 30min), 用于 API 鉴权 (放在 Authorization header)</li>
 *   <li>refresh token: 长期 (e.g. 7d), 用于刷新 access, 存数据库 (可撤销)</li>
 *   <li>refresh token 用 SHA-256 hash 存数据库, 不存明文 (泄露后无法重放)</li>
 * </ul>
 *
 * <h2>安全</h2>
 * <ul>
 *   <li>密码 BCrypt 加密 (Spring Security 自带)</li>
 *   <li>refresh token 随机生成 + hash 存储</li>
 *   <li>登录日志记录 IP/UA, 便于审计 + 异地登录告警</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.0
 */
@Slf4j  // Lombok: 自动生成 log 字段
@Service  // Spring: 注册为 Service Bean
@RequiredArgsConstructor  // Lombok: 自动构造注入 (final 字段)
public class AuthServiceImpl implements AuthService {

    // ============== 依赖注入 ==============

    /** 用户 Mapper (查/改 sys_user 表) */
    private final SysUserMapper userMapper;

    /** 用户角色关联 Mapper (查/改 sys_user_role 表) */
    private final SysUserRoleMapper userRoleMapper;

    /** 刷新令牌 Mapper (查/撤销 auth_refresh_token 表) */
    private final AuthRefreshTokenMapper refreshMapper;

    /** 登录日志 Mapper (写 auth_login_log 表) */
    private final AuthLoginLogMapper loginLogMapper;

    /** BCrypt 密码编码器 (Spring Security 提供) */
    private final PasswordEncoder passwordEncoder;

    /** JWT 签发/解析 (HS256) */
    private final JwtTokenProvider jwt;

    /** JWT 配置 (access/refresh TTL 等) */
    private final JwtProperties jwtProps;

    // ============== 业务常量 ==============

    /** 默认用户角色 ID (USER 角色, 与 init.sql 对齐) */
    private static final Long DEFAULT_USER_ROLE_ID = 2L;

    /** User-Agent 最大长度 (防止恶意超长 UA 入库) */
    private static final int MAX_USER_AGENT_LENGTH = 500;

    // ============== 注册 ==============

    /**
     * 用户注册
     *
     * <p>流程:
     * <ol>
     *   <li>校验用户名是否已存在 (抛 USER_EXISTS)</li>
     *   <li>BCrypt 加密密码</li>
     *   <li>插入 sys_user, status=1 (启用), tenantId=0 (默认租户)</li>
     *   <li>插入 sys_user_role (默认 USER 角色)</li>
     *   <li>写登录日志 (status=1, 消息"注册成功")</li>
     *   <li>直接签发 token (注册即登录体验)</li>
     * </ol>
     *
     * @param req  注册请求 {username, password, nickname, email}
     * @param http HTTP 请求 (拿 IP/UA 用于日志)
     * @return 登录响应 (含 access/refresh token)
     * @throws BizException 用户名已存在
     */
    @Override
    @Transactional  // 注册涉及多表 (user + user_role), 事务保证原子性
    public LoginResponse register(RegisterRequest req, HttpServletRequest http) {
        // 1. 校验用户名唯一
        SysUser exist = userMapper.selectByUsername(req.getUsername());
        if (exist != null) {
            throw new BizException(ResultCode.USER_EXISTS);
        }

        // 2. 构造新用户
        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        // BCrypt 加密, 每次盐值不同, 同样密码 hash 也不同
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        // 昵称缺省用用户名
        u.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        u.setEmail(req.getEmail());
        u.setStatus(1);  // 1=启用
        u.setTenantId(0L);  // 0=默认租户 (多租户预留)
        userMapper.insert(u);

        // 3. 关联默认 USER 角色
        SysUserRole ur = new SysUserRole();
        ur.setUserId(u.getId());
        ur.setRoleId(DEFAULT_USER_ROLE_ID);
        userRoleMapper.insert(ur);

        // 4. 写登录日志 (status=1 成功)
        recordLog(u.getId(), u.getUsername(), http, 1, "注册成功");

        // 5. 注册即登录体验, 直接签发 token
        return buildLoginResponse(u);
    }

    // ============== 登录 ==============

    /**
     * 用户登录 (用户名 + 密码)
     *
     * <p>流程:
     * <ol>
     *   <li>查用户 (不存在抛 USER_PASSWORD_ERROR, 防止用户名枚举)</li>
     *   <li>校验状态 (status=0 禁用, 抛 USER_DISABLED)</li>
     *   <li>校验密码 (BCrypt 匹配, 失败抛 USER_PASSWORD_ERROR)</li>
     *   <li>更新 lastLoginIp/At</li>
     *   <li>写登录日志 (成功/失败都写)</li>
     *   <li>签发 access/refresh token</li>
     * </ol>
     *
     * <p>安全: 用户不存在和密码错误返相同错误码 (USER_PASSWORD_ERROR)
     * 防止攻击者通过错误码区分"用户存在 vs 密码错"
     *
     * @param req  登录请求 {username, password}
     * @param http HTTP 请求
     * @return 登录响应
     * @throws BizException 用户不存在 / 密码错 / 账号禁用
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest req, HttpServletRequest http) {
        // 1. 查用户
        SysUser u = userMapper.selectByUsername(req.getUsername());
        if (u == null) {
            // 写失败日志 (userId=null, 未知用户)
            recordLog(null, req.getUsername(), http, 0, "用户不存在");
            // 统一错误码, 防止用户名枚举
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 2. 校验状态: null 也算禁用 (防御性编程)
        if (u.getStatus() == null || u.getStatus() == 0) {
            recordLog(u.getId(), u.getUsername(), http, 0, "账号已禁用");
            throw new BizException(ResultCode.USER_DISABLED);
        }

        // 3. 校验密码: BCrypt 自动从 hash 提取盐值 + 匹配
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            recordLog(u.getId(), u.getUsername(), http, 0, "密码错误");
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 4. 更新最后登录信息 (审计 + 异地登录检测)
        u.setLastLoginIp(IpUtils.getClientIp(http));
        u.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(u);

        // 5. 写成功日志
        recordLog(u.getId(), u.getUsername(), http, 1, "登录成功");

        // 6. 签发 token + 返响应
        return buildLoginResponse(u);
    }

    // ============== 刷新令牌 ==============

    /**
     * 刷新 access token (用 refresh token)
     *
     * <p>流程:
     * <ol>
     *   <li>把 refresh token 算 SHA-256 hash, 查数据库</li>
     *   <li>校验 refresh token (存在/未撤销/未过期)</li>
     *   <li>撤销旧 refresh token (一次性, 防重放)</li>
     *   <li>签发新 access + refresh</li>
     * </ol>
     *
     * <p>为什么用 hash 查: 数据库不存明文, 泄露后无法重放
     *
     * @param refreshTokenRaw 客户端传来的 refresh token (明文)
     * @param http            HTTP 请求
     * @return 新登录响应
     * @throws BizException token 失效/过期
     */
    @Override
    @Transactional
    public LoginResponse refresh(String refreshTokenRaw, HttpServletRequest http) {
        // 1. hash 化后查数据库
        String hash = jwt.hashRefreshToken(refreshTokenRaw);
        AuthRefreshToken row = refreshMapper.selectOne(
                new LambdaQueryWrapper<AuthRefreshToken>().eq(AuthRefreshToken::getToken, hash));

        // 2. 校验 refresh token: 存在/未撤销/未过期
        if (row == null || row.getRevoked() == 1 || row.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.USER_TOKEN_EXPIRED, "刷新令牌已失效，请重新登录");
        }

        // 3. 查用户
        SysUser u = userMapper.selectById(row.getUserId());
        if (u == null) {
            // token 关联的用户不存在 (极端情况: 用户被删)
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }

        // 4. 撤销旧 refresh token (一次性, 防重放)
        refreshMapper.revokeByToken(hash);
        recordLog(u.getId(), u.getUsername(), http, 1, "刷新令牌");

        // 5. 签发新 access + refresh
        return buildLoginResponse(u);
    }

    // ============== 登出 ==============

    /**
     * 用户登出
     *
     * <p>两种登出:
     * <ul>
     *   <li>传 refreshTokenRaw: 撤销该 token (单设备登出)</li>
     *   <li>只传 userId: 撤销该用户所有 token (全设备登出)</li>
     * </ul>
     *
     * <p>access token 是无状态的, 登出后只能等它自然过期
     * 真正的"立即失效"需要黑名单, 当前实现简化
     *
     * @param userId           用户 ID
     * @param refreshTokenRaw  refresh token (明文), 可选
     */
    @Override
    public void logout(Long userId, String refreshTokenRaw) {
        // 优先级: 有 token 就只撤销该 token
        if (refreshTokenRaw != null && !refreshTokenRaw.isBlank()) {
            refreshMapper.revokeByToken(jwt.hashRefreshToken(refreshTokenRaw));
        } else if (userId != null) {
            // 没传 token 就撤销该用户所有 (全设备登出)
            refreshMapper.revokeByUserId(userId);
        }
    }

    // ============== 查询当前用户 ==============

    /**
     * 获取当前登录用户信息 (含角色)
     *
     * @param userId 用户 ID (从 access token 解析)
     * @return 用户信息 VO
     * @throws BizException 用户不存在
     */
    @Override
    public LoginResponse.UserInfo me(Long userId) {
        // 1. 查用户
        SysUser u = userMapper.selectById(userId);
        if (u == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }

        // 2. 查角色 codes (如 ["USER", "ADMIN"])
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);

        // 3. 判断超管 (角色包含 SUPER_ADMIN)
        boolean superAdmin = roles != null && roles.contains("SUPER_ADMIN");

        // 4. 构造 VO
        return LoginResponse.UserInfo.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .email(u.getEmail())
                .avatar(u.getAvatar())
                .roles(roles)
                .superAdmin(superAdmin)
                .build();
    }

    // ============== 给非 Web 场景 (微信/OAuth) ==============

    /**
     * 给微信扫码 / OAuth / SSO 等场景生成登录响应
     *
     * <p>和 login 的区别: 不写登录日志 (微信扫码没有"密码错误"场景)
     *
     * @param u      已认证的 SysUser 实体
     * @param ignore HTTP 请求 (忽略, 兼容接口)
     * @return 登录响应
     */
    @Override
    public LoginResponse issueLoginResponse(SysUser u, HttpServletRequest ignore) {
        // 复用 buildLoginResponse
        return buildLoginResponse(u);
    }

    // ============== 私有辅助方法 ==============

    /**
     * 构造登录响应 (核心: 签 access + refresh token)
     *
     * <p>流程:
     * <ol>
     *   <li>签发 access token (短期, 30min)</li>
     *   <li>签发 refresh token (长期, 7d, 随机串)</li>
     *   <li>refresh token hash 存库, 用于后续撤销</li>
     *   <li>构造 LoginResponse VO</li>
     * </ol>
     *
     * @param u 已认证用户
     * @return 登录响应
     */
    private LoginResponse buildLoginResponse(SysUser u) {
        // 1. 查角色 (用于 access token claim + VO)
        List<String> roles = userMapper.selectRoleCodesByUserId(u.getId());
        boolean superAdmin = roles != null && roles.contains("SUPER_ADMIN");

        // 2. 签发 access token (短期)
        String access = jwt.issueAccessToken(u.getId(), u.getUsername(), roles);

        // 3. 签发 refresh token (长期, 随机串)
        String refresh = jwt.issueRefreshToken();

        // 4. refresh token hash 存库 (用于后续撤销 + 刷新校验)
        AuthRefreshToken row = new AuthRefreshToken();
        row.setUserId(u.getId());
        row.setToken(jwt.hashRefreshToken(refresh));  // 存 hash, 不存明文
        row.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProps.getRefreshTtlSeconds()));
        row.setRevoked(0);  // 0=未撤销
        refreshMapper.insert(row);

        // 5. 构造响应 VO
        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(jwtProps.getAccessTtlSeconds())
                .tokenType("Bearer")
                .user(LoginResponse.UserInfo.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .email(u.getEmail())
                        .avatar(u.getAvatar())
                        .roles(roles)
                        .superAdmin(superAdmin)
                        .build())
                .build();
    }

    /**
     * 写登录日志 (容错: 写失败不阻塞主流程)
     *
     * <p>登录日志是审计 + 异常检测的关键:
     * <ul>
     *   <li>status=1: 成功</li>
     *   <li>status=0: 失败 (用户名错/密码错/账号禁用)</li>
     * </ul>
     *
     * <p>异常处理: 写日志失败也不能影响主流程 (e.g. 用户已经登录成功)
     *
     * @param userId   用户 ID (可空, 未知用户)
     * @param username 用户名 (用于审计)
     * @param http     HTTP 请求 (拿 IP/UA)
     * @param status   1=成功, 0=失败
     * @param msg      日志消息
     */
    private void recordLog(Long userId, String username, HttpServletRequest http, int status, String msg) {
        // try-catch: 写日志失败不能让主流程挂
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
            // 写日志失败只 warn, 不抛异常 (业务已成功)
            log.warn("写登录日志失败: {}", e.getMessage());
        }
    }

    /**
     * 安全获取 User-Agent (截断过长 UA)
     *
     * <p>防御: UA 头可能被恶意客户端塞超长字符串, 防止 DB 字段溢出
     *
     * @param http HTTP 请求
     * @return User-Agent (截断到 MAX_USER_AGENT_LENGTH), 无 UA 返 null
     */
    private String safeUa(HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        if (ua == null) {
            return null;
        }
        // 超长截断 (substring 不会 OOM, 因为 ua 是 String 已在内存)
        return ua.length() > MAX_USER_AGENT_LENGTH
                ? ua.substring(0, MAX_USER_AGENT_LENGTH)
                : ua;
    }
}
