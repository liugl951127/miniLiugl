package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体。
 * 字段命名严格对齐 sys_user 表，避免歧义。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    /** BCrypt 哈希，禁止明文落库。 */
    @TableField("password")
    private String password;

    @TableField("nickname")
    private String nickname;
    @TableField("email")
    private String email;
    @TableField("phone")
    private String phone;
    @TableField("avatar")
    private String avatar;

    /** 0未知 1男 2女 */
    @TableField("gender")
    private Integer gender;

    /** 0禁用 1正常 */
    @TableField("status")
    private Integer status;

    @TableField("last_login_ip")
    private String lastLoginIp;
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    @TableField("tenant_id")
    private Long tenantId;
    @TableField("remark")
    private String remark;

    // ============ V5: 微信扫码登录 ============
    /** 微信 openid (公众号/小程序唯一) */
    @TableField("wechat_openid")
    private String wechatOpenid;
    /** 微信 unionid (开放平台跨应用唯一) */
    @TableField("wechat_unionid")
    private String wechatUnionid;
    /** 微信昵称 (冗余) */
    @TableField("wechat_nickname")
    private String wechatNickname;
    /** 微信头像 URL */
    @TableField("wechat_avatar")
    private String wechatAvatar;
    /** 微信绑定时间 */
    @TableField("wechat_bound_at")
    private LocalDateTime wechatBoundAt;

    // ============ V5.2: QQ/支付宝 跨平台 ============
    /** QQ openid */
    @TableField("qq_openid")
    private String qqOpenid;
    /** QQ unionid (QQ互联跨应用唯一) */
    @TableField("qq_unionid")
    private String qqUnionid;
    /** QQ 昵称 */
    @TableField("qq_nickname")
    private String qqNickname;
    /** QQ 头像 URL */
    @TableField("qq_avatar")
    private String qqAvatar;
    /** QQ 绑定时间 */
    @TableField("qq_bound_at")
    private LocalDateTime qqBoundAt;

    /** 支付宝 openid (用户标识) */
    @TableField("alipay_openid")
    private String alipayOpenid;
    /** 支付宝 user_id (应用授权令牌返回) */
    @TableField("alipay_user_id")
    private String alipayUserId;
    /** 支付宝昵称 */
    @TableField("alipay_nickname")
    private String alipayNickname;
    /** 支付宝头像 URL */
    @TableField("alipay_avatar")
    private String alipayAvatar;
    /** 支付宝绑定时间 */
    @TableField("alipay_bound_at")
    private LocalDateTime alipayBoundAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
