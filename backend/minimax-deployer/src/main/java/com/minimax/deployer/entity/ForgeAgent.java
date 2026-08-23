package com.minimax.deployer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Forge Agent (V4.1)
 *
 * V4.1 关键改动: 删 project_id 字段
 *  - parse 阶段不写表 (只返回响应给前端展示)
 *  - release 创建时写一次, 绑 release_id
 *  - 解决 V4.0 双写问题 (parse 一行 + release 一行)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_agent")
public class ForgeAgent implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("release_id")
    private Long releaseId;

    @TableField("name")
    private String name;

    @TableField("role")
    private String role;

    @TableField("emoji")
    private String emoji;

    @TableField("description")
    private String description;

    @TableField("color")
    private String color;

    @TableField("tools")
    private String tools;

    @TableField("model")
    private String model;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
