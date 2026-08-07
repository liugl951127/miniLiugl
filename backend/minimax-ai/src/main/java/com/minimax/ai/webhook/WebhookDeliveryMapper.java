package com.minimax.ai.webhook;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * WebhookDeliveryMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * Webhook - WebhookDeliveryMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 WebhookDeliveryMapper 的业务能力</li>
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
public interface WebhookDeliveryMapper extends BaseMapper<WebhookDelivery> {

    @Select("SELECT * FROM webhook_delivery WHERE webhookId = #{webhookId} ORDER BY createdAt DESC LIMIT #{limit}")
    List<WebhookDelivery> findByWebhookId(@Param("webhookId") String webhookId, @Param("limit") int limit);

    @Select("SELECT * FROM webhook_delivery ORDER BY createdAt DESC LIMIT #{limit}")
    List<WebhookDelivery> findRecent(@Param("limit") int limit);
}
