package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_channel")
public class AlertChannel {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;
    @TableField("channel_type")
    private String channelType;       // email/dingtalk/wechat/webhook/sms
    @TableField("type")
    private String type;               // 别名 (兼容)
    @TableField("target")
    private String target;
    @TableField("config")
    private String config;
    @TableField("enabled")
    private Integer enabled;
    @TableField("priority")
    private Integer priority;          // 通知优先级
    @TableField("description")
    private String description;

    /** 告警通知模板 (Day 28). 支持变量: ${ruleName} ${severity} ${metricName} ${metricValue} ${threshold} ${message} ${firedAt} ${service} */
    @TableField("template")
    private String template;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public String getChannelType() { return channelType != null ? channelType : type; }
    public void setChannelType(String t) { this.channelType = t; this.type = t; }
}
