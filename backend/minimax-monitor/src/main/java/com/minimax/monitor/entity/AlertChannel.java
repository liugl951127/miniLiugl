package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_channel")
public class AlertChannel {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String channelType;       // email/dingtalk/wechat/webhook/sms
    private String type;               // 别名 (兼容)
    private String target;
    private String config;
    private Integer enabled;
    private Integer priority;          // 通知优先级
    private String description;

    /** 告警通知模板 (Day 28). 支持变量: ${ruleName} ${severity} ${metricName} ${metricValue} ${threshold} ${message} ${firedAt} ${service} */
    private String template;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public String getChannelType() { return channelType != null ? channelType : type; }
    public void setChannelType(String t) { this.channelType = t; this.type = t; }
}
