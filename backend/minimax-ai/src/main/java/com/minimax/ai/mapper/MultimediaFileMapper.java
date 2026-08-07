package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.MultimediaFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * MultimediaFileMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * MyBatis Mapper - MultimediaFileMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 MultimediaFileMapper 的业务能力</li>
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
public interface MultimediaFileMapper extends BaseMapper<MultimediaFile> {
}
