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
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 启动时确保默认 admin 账号存在且密码用 BCrypt 正确编码。
 * 避免 H2 schema 里的硬编码 hash 因字符集问题截断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 角色
        SysRole adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "ADMIN"));
        if (adminRole == null) {
            adminRole = new SysRole();
            adminRole.setCode("ADMIN");
            adminRole.setName("超级管理员");
            adminRole.setSort(1);
            adminRole.setEnabled(1);
            roleMapper.insert(adminRole);
        }
        SysRole userRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "USER"));
        if (userRole == null) {
            userRole = new SysRole();
            userRole.setCode("USER");
            userRole.setName("普通用户");
            userRole.setSort(2);
            userRole.setEnabled(1);
            roleMapper.insert(userRole);
        }

        // admin 用户
        SysUser admin = userMapper.selectByUsername("admin");
        if (admin == null) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin@123"));
            admin.setNickname("超级管理员");
            admin.setStatus(1);
            admin.setTenantId(0L);
            userMapper.insert(admin);
            log.info("✅ 初始化 admin 用户 (密码 admin@123)");

            // 绑定 ADMIN 角色
            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(adminRole.getId());
            userRoleMapper.insert(ur);
        }
    }
}
