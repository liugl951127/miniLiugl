package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    private String username;

    /** BCrypt 哈希，禁止明文落库。 */
    private String password;

    private String nickname;
    private String email;
    private String phone;
    private String avatar;

    /** 0未知 1男 2女 */
    private Integer gender;

    /** 0禁用 1正常 */
    private Integer status;

    private String lastLoginIp;
    private LocalDateTime lastLoginAt;
    private Long tenantId;
    private String remark;

    // ============ V5: 微信扫码登录 ============
    /** 微信 openid (公众号/小程序唯一) */
    private String wechatOpenid;
    /** 微信 unionid (开放平台跨应用唯一) */
    private String wechatUnionid;
    /** 微信昵称 (冗余) */
    private String wechatNickname;
    /** 微信头像 URL */
    private String wechatAvatar;
    /** 微信绑定时间 */
    private LocalDateTime wechatBoundAt;

    // ============ V5.2: QQ/支付宝 跨平台 ============
    /** QQ openid */
    private String qqOpenid;
    /** QQ unionid (QQ互联跨应用唯一) */
    private String qqUnionid;
    /** QQ 昵称 */
    private String qqNickname;
    /** QQ 头像 URL */
    private String qqAvatar;
    /** QQ 绑定时间 */
    private LocalDateTime qqBoundAt;

    /** 支付宝 openid (用户标识) */
    private String alipayOpenid;
    /** 支付宝 user_id (应用授权令牌返回) */
    private String alipayUserId;
    /** 支付宝昵称 */
    private String alipayNickname;
    /** 支付宝头像 URL */
    private String alipayAvatar;
    /** 支付宝绑定时间 */
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
