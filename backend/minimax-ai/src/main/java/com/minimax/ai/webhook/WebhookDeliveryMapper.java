package com.minimax.ai.webhook;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WebhookDeliveryMapper extends BaseMapper<WebhookDelivery> {

    @Select("SELECT * FROM webhook_delivery WHERE webhookId = #{webhookId} ORDER BY createdAt DESC LIMIT #{limit}")
    List<WebhookDelivery> findByWebhookId(@Param("webhookId") String webhookId, @Param("limit") int limit);

    @Select("SELECT * FROM webhook_delivery ORDER BY createdAt DESC LIMIT #{limit}")
    List<WebhookDelivery> findRecent(@Param("limit") int limit);
}
