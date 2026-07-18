package com.minimax.auth.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.entity.SysRole;
import com.minimax.auth.entity.SysUser;
import com.minimax.auth.entity.SysUserRole;
import com.minimax.auth.mapper.SysRoleMapper;
import com.minimax.auth.mapper.SysUserMapper;
import com.minimax.auth.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 启动时确保默认账号和角色存在 (V3.5.12+).
 *
 * <p>V3.5.12 之前, 默认账号由 seed-data.sql 创建. 沙箱模式不跑 seed,
 * 所以本类改为: 启动时通过 CommandLineRunner 兜底创建.</p>
 *
 * <p>本类只依赖 entity 和 mapper, 不用 SQL, 不会被 schema 错位阻塞.</p>
 *
 * <h3>默认账号</h3>
 * <ul>
 *   <li>adminLiugl  / Liugl@2026  (SUPER_ADMIN, 平台所有者)</li>
 *   <li>admin        / admin@123   (ADMIN, 普通管理员 — 兼容旧版本)</li>
 *   <li>admin_user   / admin123    (ADMIN, 沙箱测试账号 — V3.5.12+ 新增)</li>
 *   <li>test_user    / user123     (USER, 沙箱测试账号 — V3.5.12+ 新增)</li>
 *   <li>demo_user    / demo1234    (USER, demo 租户 — V3.5.12+ 新增)</li>
 * </ul>
 *
 * <h3>修复记录</h3>
 * <ul>
 *   <li>V3.5.8: 初版, 只建 admin + adminLiugl</li>
 *   <li>V3.5.12: 沙箱不跑 seed, 扩展为 5 账号兜底; 加 setDeleted(0) 解决 @TableLogic 过滤</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${minimax.auth.super-admin-username:adminLiugl}")
    private String superAdminUsername;

    @Value("${minimax.auth.super-admin-password:Liugl@2026}")
    private String superAdminPassword;

    @Value("${minimax.auth.super-admin-email:liugl951127@gmail.com}")
    private String superAdminEmail;

    @Override
    @Transactional
    public void run(String... args) {
        // ---------- 1. 角色 ----------
        SysRole superRole = ensureRole("SUPER_ADMIN", "超级管理员 (adminLiugl)",
                "拥有平台所有权限, 包括管理其他管理员", 0);
        SysRole adminRole = ensureRole("ADMIN", "管理员",
                "管理员, 拥有大部分业务权限", 1);
        SysRole userRole = ensureRole("USER", "普通用户",
                "普通用户, 受限权限", 2);

        // ---------- 2. adminLiugl (唯一超级管理员) ----------
        ensureSuperAdmin(superRole);

        // ---------- 3. admin (旧版普通管理员, 兼容) ----------
        ensureAdminUser("admin", "admin@123", "管理员 (旧版)", adminRole);

        // ---------- 4. V3.5.12+: 4 测试账号 (admin_user/test_user/demo_user) ----------
        // 之前由 seed-data.sql 创建, 沙箱不跑 seed 后改由本类兜底
        ensureTestUser("admin_user", "admin123", "管理员",
                emailOf("admin"), adminRole, 0L);
        ensureTestUser("test_user",  "user123",  "测试用户",
                emailOf("test"),  userRole,  0L);
        ensureTestUser("demo_user",  "demo1234", "演示用户 (Demo 租户)",
                emailOf("demo"),  userRole,  2L);

        log.info("✅ AdminDataInitializer 兜底完成 (V3.5.12+ 沙箱不跑 seed)");
    }

    /**
     * 创建或更新超级管理员.
     */
    private void ensureSuperAdmin(SysRole superRole) {
        SysUser owner = userMapper.selectByUsername(superAdminUsername);
        if (owner == null) {
            owner = new SysUser();
            owner.setUsername(superAdminUsername);
            owner.setPassword(passwordEncoder.encode(superAdminPassword));
            owner.setNickname("Liugl (Owner)");
            owner.setEmail(superAdminEmail);
            owner.setStatus(1);
            owner.setTenantId(0L);
            owner.setDeleted(0);  // V3.5.12+: 显式设 0, 避免 @TableLogic 过滤
            owner.setRemark("平台所有者 - 唯一超级管理员");
            userMapper.insert(owner);
            log.info("✅ 初始化超级管理员 {} (密码 {})", superAdminUsername, superAdminPassword);
            bindRole(owner.getId(), superRole.getId());
        } else {
            // 存在则更新密码/邮箱 (确保密码始终正确)
            owner.setPassword(passwordEncoder.encode(superAdminPassword));
            owner.setEmail(superAdminEmail);
            owner.setStatus(1);
            owner.setDeleted(0);
            userMapper.updateById(owner);
            if (!hasRole(owner.getId(), superRole.getId())) {
                bindRole(owner.getId(), superRole.getId());
            }
            log.info("🔄 已同步超级管理员 {} (密码已重置)", superAdminUsername);
        }
    }

    /**
     * 创建或更新一个普通管理员/用户 (旧版兼容).
     */
    private void ensureAdminUser(String username, String password, String nickname, SysRole role) {
        ensureTestUser(username, password, nickname, emailOf(username), role, 0L);
    }

    /**
     * V3.5.12+: 兜底创建测试账号 (原 seed-data.sql 负责).
     */
    private void ensureTestUser(String username, String password, String nickname,
                                String email, SysRole role, Long tenantId) {
        SysUser u = userMapper.selectByUsername(username);
        if (u == null) {
            u = new SysUser();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(password));
            u.setNickname(nickname);
            u.setEmail(email);
            u.setStatus(1);
            u.setDeleted(0);  // V3.5.12+: 显式设 0, 避免 @TableLogic 过滤
            u.setTenantId(tenantId);
            u.setRemark("测试账号 (AdminDataInitializer V3.5.12+ 兜底)");
            userMapper.insert(u);
            log.info("✅ 初始化测试账号 {} (密码 {})", username, password);
            bindRole(u.getId(), role.getId());
        } else {
            // 已存在: 重置密码 + 状态
            u.setPassword(passwordEncoder.encode(password));
            u.setStatus(1);
            u.setDeleted(0);
            u.setEmail(email);
            u.setTenantId(tenantId);
            userMapper.updateById(u);
            if (!hasRole(u.getId(), role.getId())) {
                bindRole(u.getId(), role.getId());
            }
            log.info("🔄 已同步测试账号 {} (密码已重置)", username);
        }
    }

    /**
     * 创建或获取角色.
     */
    private SysRole ensureRole(String code, String name, String desc, int sort) {
        SysRole r = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code));
        if (r == null) {
            r = new SysRole();
            r.setCode(code);
            r.setName(name);
            r.setDescription(desc);
            r.setSort(sort);
            r.setEnabled(1);
            r.setDeleted(0);  // V3.5.12+: 同上
            roleMapper.insert(r);
            log.info("✅ 初始化角色 {} ({})", code, name);
        } else {
            r.setName(name);
            r.setDescription(desc);
            r.setSort(sort);
            r.setEnabled(1);
            r.setDeleted(0);
            roleMapper.updateById(r);
        }
        return r;
    }

    private void bindRole(Long userId, Long roleId) {
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);
    }

    private boolean hasRole(Long userId, Long roleId) {
        return userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getRoleId, roleId)) > 0;
    }

    /**
     * 拼邮箱: superAdminEmail 改成 +tag@ 形式
     * 例: liugl951127@gmail.com → liugl951127+admin@gmail.com
     */
    private String emailOf(String tag) {
        if (superAdminEmail == null || !superAdminEmail.contains("@")) {
            return tag + "@minimax.io";
        }
        return superAdminEmail.replace("@", "+" + tag + "@");
    }
}
