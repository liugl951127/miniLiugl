package com.minimax.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.entity.SysUser;
import com.minimax.auth.entity.Tenant;
import com.minimax.auth.mapper.SysUserMapper;
import com.minimax.auth.mapper.TenantMapper;
import com.minimax.common.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * V3.1: 多租户服务
 * - 实现 TenantResolver (从 sys_user 查 tenant_id)
 * - 租户 CRUD (adminLiugl 跨租户)
 * - 切换租户 (adminLiugl 专属)
 */
@Service
@RequiredArgsConstructor
public class TenantService implements TenantResolver {

    private final TenantMapper tenantMapper;
    private final SysUserMapper userMapper;

    /** 实现 TenantResolver */
    @Override
    public TenantInfo resolve(Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) return new TenantInfo(0L, "default", false);

        // 检查是否是超级管理员
        boolean superAdmin = isSuperAdmin();
        if (superAdmin) {
            return new TenantInfo(0L, "platform", true);
        }

        // 查租户
        Long tenantId = u.getTenantId() == null ? 1L : u.getTenantId();
        Tenant t = tenantMapper.selectById(tenantId);
        String code = (t == null) ? "default" : t.getCode();
        return new TenantInfo(tenantId, code, false);
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> auths = auth.getAuthorities();
        if (auths == null) return false;
        for (GrantedAuthority a : auths) {
            if ("ROLE_SUPER_ADMIN".equals(a.getAuthority())) return true;
        }
        return false;
    }

    // ---------- 租户 CRUD ----------

    public List<Tenant> listAll() {
        return tenantMapper.selectList(
                new LambdaQueryWrapper<Tenant>().orderByAsc(Tenant::getId));
    }

    public Tenant getById(Long id) {
        return tenantMapper.selectById(id);
    }

    public Tenant getByCode(String code) {
        return tenantMapper.selectByCode(code);
    }

    public Long create(String code, String name, String plan,
                       Integer maxUsers, Integer maxModels, Integer qpsLimit,
                       Long monthlyQuota, String contactEmail, String remark) {
        if (tenantMapper.selectByCode(code) != null) {
            throw new IllegalArgumentException("租户代码已存在: " + code);
        }
        Tenant t = new Tenant();
        t.setCode(code);
        t.setName(name);
        t.setPlan(plan == null ? "free" : plan);
        t.setStatus(1);
        t.setMaxUsers(maxUsers == null ? 10 : maxUsers);
        t.setMaxModels(maxModels == null ? 5 : maxModels);
        t.setQpsLimit(qpsLimit == null ? 100 : qpsLimit);
        t.setMonthlyQuota(monthlyQuota == null ? 100000L : monthlyQuota);
        t.setUsedQuota(0L);
        t.setContactEmail(contactEmail);
        t.setRemark(remark);
        tenantMapper.insert(t);
        return t.getId();
    }

    public boolean setStatus(Long id, int status) {
        Tenant t = tenantMapper.selectById(id);
        if (t == null) return false;
        t.setStatus(status);
        tenantMapper.updateById(t);
        return true;
    }

    public boolean updateQuota(Long id, Long quota) {
        Tenant t = tenantMapper.selectById(id);
        if (t == null) return false;
        t.setMonthlyQuota(quota);
        tenantMapper.updateById(t);
        return true;
    }

    public boolean delete(Long id) {
        Tenant t = tenantMapper.selectById(id);
        if (t == null) return false;
        if ("default".equals(t.getCode())) {
            throw new IllegalArgumentException("不能删除默认租户");
        }
        tenantMapper.deleteById(id);
        return true;
    }

    public List<SysUser> listTenantUsers(Long tenantId) {
        return userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getTenantId, tenantId));
    }
}
