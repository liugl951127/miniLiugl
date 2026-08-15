package com.minimax.ai.webhook;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * WebhookMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * Webhook - WebhookMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 WebhookMapper 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@Mapper
public interface WebhookMapper extends BaseMapper<Webhook> {

    @Update("UPDATE webhook SET deliveryCount = deliveryCount + 1, " +
            "successCount = successCount + #{successDelta}, " +
            "failCount = failCount + #{failDelta}, " +
            "lastDeliveryAt = NOW(), lastStatus = #{status} " +
            "WHERE webhookId = #{webhookId}")
    int updateDeliveryStats(@Param("webhookId") String webhookId,
                            @Param("successDelta") int successDelta,
                            @Param("failDelta") int failDelta,
                            @Param("status") int status);
}
