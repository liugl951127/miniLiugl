package com.minimax.ai.modelmarket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ModelMarketMapper extends BaseMapper<ModelEntry> {

    @Update("UPDATE model_market SET downloadCount = downloadCount + 1 WHERE modelKey = #{modelKey}")
    int incrementDownload(@Param("modelKey") String modelKey);

    @Update("UPDATE model_market SET " +
            "avgRating = COALESCE((SELECT AVG(rating) FROM model_rating WHERE modelKey = #{modelKey}), 0), " +
            "ratingCount = (SELECT COUNT(*) FROM model_rating WHERE modelKey = #{modelKey}) " +
            "WHERE modelKey = #{modelKey}")
    int updateRatingStats(@Param("modelKey") String modelKey);
}
