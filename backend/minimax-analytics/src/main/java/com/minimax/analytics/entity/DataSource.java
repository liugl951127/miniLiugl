package com.minimax.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源配置 (V5.31)
 *
 * 支持多数据源: 业务库/日志库/数仓, 每个用户独立配置
 * 密码字段加密存储 (AES-256)
 */
@Data
@TableName("analytics_datasource")
public class DataSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("name")
    private String name;             // 用户给的名字, 如 "生产DB"
    @TableField("type")
    private String type;             // mysql / h2 / postgresql (V5.31 仅 mysql/h2)
    @TableField("jdbc_url")
    private String jdbcUrl;          // jdbc:mysql://host:3306/db
    @TableField("username")
    private String username;
    @TableField("password_enc")
    private String passwordEnc;      // AES 加密
    @TableField("description")
    private String description;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;         // 软删除

    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
