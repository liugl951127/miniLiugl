package com.minimax.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.model.entity.ModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    /** 拉所有启用的模型，连带 provider 信息。 */
    List<Map<String, Object>> selectEnabledWithProvider();

    /** 按 code 查（含 provider）。 */
    Map<String, Object> selectByCode(@Param("code") String code);
}
