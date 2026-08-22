package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.NotificationSettings;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知设置 Mapper (T1-backend-apis / P0)
 *
 * @since V7.2
 */
@Mapper
public interface NotificationSettingsMapper extends BaseMapper<NotificationSettings> {
}
