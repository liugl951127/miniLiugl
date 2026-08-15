package com.minimax.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.agent.entity.KgRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KgRelationMapper extends BaseMapper<KgRelation> {

    /** 删除某实体所有关系 */
    @Update("UPDATE kg_relation SET deleted = 1 WHERE (from_entity = #{id} OR to_entity = #{id}) AND deleted = 0")
    int deleteByEntity(@Param("id") Long id);
}
