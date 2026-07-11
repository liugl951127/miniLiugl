package com.minimax.ai.framework.permission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 权限定义 (V2.8.6)
 *
 * <h3>设计: 敏感操作必须用户显式授权</h3>
 * <ul>
 *   <li>下单 / 支付 → order:create</li>
 *   <li>查看位置 → location:read</li>
 *   <li>读取通讯录 → contacts:read</li>
 *   <li>读取记忆 → memory:read</li>
 *   <li>删除数据 → data:delete</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    /** 权限编码 (唯一) */
    private String code;
    /** 权限名称 (展示给用户) */
    private String name;
    /** 权限描述 (为什么需要) */
    private String description;
    /** 风险等级 (LOW/MEDIUM/HIGH) */
    private String riskLevel;

    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";

    /** 常用权限 */
    public static Permission location() {
        return new Permission("location:read", "访问位置",
                "用于推荐附近的商城/酒店/娱乐场所", RISK_LOW);
    }
    public static Permission orderCreate() {
        return new Permission("order:create", "创建订单",
                "代您下单购买商品", RISK_HIGH);
    }
    public static Permission orderRead() {
        return new Permission("order:read", "查看订单",
                "查询您的历史订单", RISK_LOW);
    }
    public static Permission memoryRead() {
        return new Permission("memory:read", "读取记忆",
                "读取您之前的对话偏好", RISK_LOW);
    }
    public static Permission memoryWrite() {
        return new Permission("memory:write", "写入记忆",
                "记录您的偏好以提供个性化服务", RISK_LOW);
    }
    public static Permission contactRead() {
        return new Permission("contact:read", "读取通讯录",
                "用于推荐好友相关商品", RISK_MEDIUM);
    }
    public static Permission payment() {
        return new Permission("payment", "支付",
                "代您完成支付", RISK_HIGH);
    }
}
