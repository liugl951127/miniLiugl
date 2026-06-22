package com.minimax.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    private Long userId;
    private String name;             // 用户给的名字, 如 "生产DB"
    private String type;             // mysql / h2 / postgresql (V5.31 仅 mysql/h2)
    private String jdbcUrl;          // jdbc:mysql://host:3306/db
    private String username;
    private String passwordEnc;      // AES 加密
    private String description;

    @TableLogic
    private Integer deleted;         // 软删除

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
