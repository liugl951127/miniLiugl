package com.minimax.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.system.entity.SystemSettings;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统设置 Mapper (T1-backend-apis / P0)
 *
 * @since V7.2
 */
@Mapper
public interface SystemSettingsMapper extends BaseMapper<SystemSettings> {
}
