package com.minimax.ai.modelmarket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ModelRatingMapper extends BaseMapper<ModelRating> {

    @Select("SELECT * FROM model_rating WHERE modelKey = #{modelKey} ORDER BY createdAt DESC LIMIT #{limit}")
    List<ModelRating> findByModelKey(@Param("modelKey") String modelKey, @Param("limit") int limit);

    @Select("SELECT * FROM model_rating WHERE modelKey = #{modelKey} AND userId = #{userId} LIMIT 1")
    ModelRating findUserRating(@Param("modelKey") String modelKey, @Param("userId") Long userId);
}
