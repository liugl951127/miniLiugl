package com.minimax.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统全局设置 (T1-backend-apis / P0)
 *
 * 单行表 (id=1), upsert 模式持久化。
 * 修复 views/settings/Index.vue saveSysSettings() 的"仅本地生效"问题。
 *
 * @since V7.2
 */
@Data
@TableName("system_settings")
public class SystemSettings {

    /** 单行表的固定 ID, 永远=1 */
    @TableId(type = IdType.INPUT)
    private Long id;

    private String siteName;
    private String siteLogo;

    /** 0=正常 1=维护模式 */
    private Integer maintenanceMode;

    /** 0=禁止注册 1=允许注册 */
    private Integer allowRegister;

    /** 默认模型编码 (e.g. gpt-4o) */
    private String defaultModelCode;

    private String description;
    private String contactEmail;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
