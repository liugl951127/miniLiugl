package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * unionid 跨应用账号关联 (V5.1).
 *
 * 跨应用场景:
 *   - 同一微信开放平台下, 公众号 + 小程序 + App 共享 unionid
 *   - 一个 unionid 可对应同一平台账号的多个应用
 *   - 支持手动作账号合并 (admin)
 *
 * @since 2026-06
 */
@Data
@TableName("unionid_relations")
public class UnionidRelations {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String unionid;

    private String platform;

    private LocalDateTime firstSeenAt;

    private LocalDateTime lastSeenAt;

    private Integer bindingCount;
}