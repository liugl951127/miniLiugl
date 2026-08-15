package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI投票记录表 (V6.8.10 Day 39)
 *
 * <p>记录每次多模型投票的完整信息：
 * <ul>
 *   <li>投票问题与最终答案</li>
 *   <li>各模型投票详情 (JSON)</li>
 *   <li>投票策略与一致率</li>
 * </ul>
 *
 * @author Mavis
 * @since V6.8.10
 */
@Data
@TableName("ai_voting_record")
public class AiVotingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 投票问题文本 */
    private String question;

    /** 最终答案 (A/B/C/D) */
    private String finalAnswer;

    /** 投票策略 (majority/weighted/random) */
    private String strategy;

    /** 参与模型数 */
    private Integer totalVotes;

    /** 一致率 (0.0000-1.0000) */
    private BigDecimal agreementRate;

    /**
     * 各模型投票 JSON
     * 例: [{"model":"gpt-4","answer":"A","confidence":0.92}]
     */
    private String modelVotes;

    /** 投票耗时ms */
    private Integer durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 投票结束时通知邮箱 (Day 43) */
    private String notifyEmail;
}
