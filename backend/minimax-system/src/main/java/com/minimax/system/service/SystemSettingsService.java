package com.minimax.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.common.exception.BizException;
import com.minimax.system.entity.SystemSettings;
import com.minimax.system.mapper.SystemSettingsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 系统设置服务 (T1-backend-apis / P0)
 *
 * 2 个端点 (修复 views/settings/Index.vue saveSysSettings() 的"仅本地生效"):
 *   - GET  /api/v1/system/settings  拿单行 (id=1), 无则返回默认
 *   - PUT  /api/v1/system/settings  upsert (id 永远=1)
 *
 * @since V7.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private static final long SINGLETON_ID = 1L;
    private static final Set<String> ALLOWED_BOOL = Set.of("0", "1");

    private final SystemSettingsMapper settingsMapper;

    /**
     * 拿当前系统设置 (无则返回默认)
     */
    public SystemSettings get() {
        SystemSettings s = settingsMapper.selectById(SINGLETON_ID);
        if (s == null) {
            s = defaultSettings();
        }
        return s;
    }

    /**
     * upsert 设置
     */
    @Transactional
    public SystemSettings upsert(SystemSettings patch, Long updatedBy) {
        if (patch == null) {
            throw new BizException("请求体不能为空");
        }
        // 简单校验
        if (patch.getMaintenanceMode() != null && !ALLOWED_BOOL.contains(String.valueOf(patch.getMaintenanceMode()))) {
            throw new BizException("maintenanceMode 只能为 0 或 1");
        }
        if (patch.getAllowRegister() != null && !ALLOWED_BOOL.contains(String.valueOf(patch.getAllowRegister()))) {
            throw new BizException("allowRegister 只能为 0 或 1");
        }
        SystemSettings exist = settingsMapper.selectById(SINGLETON_ID);
        if (exist == null) {
            exist = defaultSettings();
            applyPatch(exist, patch);
            exist.setUpdatedBy(updatedBy);
            settingsMapper.insert(exist);
            log.info("[SystemSettings] created (singleton) by userId={}", updatedBy);
        } else {
            applyPatch(exist, patch);
            exist.setUpdatedBy(updatedBy);
            // updatedAt 由 MetaObjectHandler 自动填充
            settingsMapper.updateById(exist);
            log.info("[SystemSettings] updated (singleton) by userId={}", updatedBy);
        }
        return settingsMapper.selectById(SINGLETON_ID);
    }

    private SystemSettings defaultSettings() {
        SystemSettings s = new SystemSettings();
        s.setId(SINGLETON_ID);
        s.setSiteName("MiniMax 平台");
        s.setMaintenanceMode(0);
        s.setAllowRegister(1);
        s.setDefaultModelCode("gpt-4o");
        return s;
    }

    private void applyPatch(SystemSettings target, SystemSettings patch) {
        if (patch.getSiteName() != null) target.setSiteName(patch.getSiteName());
        if (patch.getSiteLogo() != null) target.setSiteLogo(patch.getSiteLogo());
        if (patch.getMaintenanceMode() != null) target.setMaintenanceMode(patch.getMaintenanceMode());
        if (patch.getAllowRegister() != null) target.setAllowRegister(patch.getAllowRegister());
        if (patch.getDefaultModelCode() != null) target.setDefaultModelCode(patch.getDefaultModelCode());
        if (patch.getDescription() != null) target.setDescription(patch.getDescription());
        if (patch.getContactEmail() != null) target.setContactEmail(patch.getContactEmail());
    }
}
