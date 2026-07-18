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
 * 启动时确保默认账号存在且密码用 BCrypt 正确编码。
 * 避免 H2 schema 里的硬编码 hash 因字符集问题截断。
 *
 * 默认账号:
 *   - admin        / admin@123      (普通管理员, ADMIN 角色)
 *   - adminLiugl   / Liugl@2026     (超级管理员, SUPER_ADMIN 角色 - 平台所有者)
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
        // ---------- 角色 ----------
        SysRole superRole = ensureRole("SUPER_ADMIN", "超级管理员 (adminLiugl)",
                "拥有平台所有权限, 包括管理其他管理员", 0);
        SysRole adminRole = ensureRole("ADMIN", "超级管理员", "管理员", 1);
        SysRole userRole = ensureRole("USER", "普通用户", "普通用户", 2);

        // ---------- admin 用户 (普通管理员) ----------
        SysUser admin = userMapper.selectByUsername("admin");
        if (admin == null) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin@123"));
            admin.setNickname("超级管理员");
            admin.setStatus(1);
            admin.setDeleted(0);  // V3.5.12+ 显式 0
            admin.setTenantId(0L);
            userMapper.insert(admin);
            log.info("✅ 初始化 admin 用户 (密码 admin@123)");

            bindRole(admin.getId(), adminRole.getId());
        }

        // ---------- adminLiugl 用户 (唯一超级管理员) ----------
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
            log.info("✅ 初始化超级管理员 {} (密码 {}) hash={}", superAdminUsername, superAdminPassword, owner.getPassword());

            bindRole(owner.getId(), superRole.getId());
        } else {
            // 存在则更新密码/邮箱 (确保密码始终正确)
            owner.setPassword(passwordEncoder.encode(superAdminPassword));
            owner.setEmail(superAdminEmail);
            owner.setStatus(1);
            userMapper.updateById(owner);

            // 确保绑定 SUPER_ADMIN 角色
            if (!hasRole(owner.getId(), superRole.getId())) {
                bindRole(owner.getId(), superRole.getId());
            }
            log.info("🔄 已同步超级管理员 {} (密码已重置)", superAdminUsername);
        }
    }

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
            roleMapper.insert(r);
        } else {
            // 始终更新 name/desc (允许 schema 改变后同步)
            r.setName(name);
            r.setDescription(desc);
            r.setEnabled(1);
            r.setSort(sort);
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
}
