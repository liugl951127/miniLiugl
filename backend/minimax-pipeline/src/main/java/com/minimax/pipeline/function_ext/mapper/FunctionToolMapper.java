package com.minimax.pipeline.function_ext.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.pipeline.function_ext.entity.FunctionTool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FunctionToolMapper extends BaseMapper<FunctionTool> {

    @Select("SELECT * FROM function_tool WHERE name = #{name} AND deleted = 0 LIMIT 1")
    FunctionTool selectByName(@Param("name") String name);

    /** T2: 批量按 name 查 (消除 N+1) — 使用 LambdaQueryWrapper 实现 */
    default List<FunctionTool> selectBatchByNames(java.util.Collection<String> names) {
        if (names == null || names.isEmpty()) return java.util.Collections.emptyList();
        return selectList(new LambdaQueryWrapper<FunctionTool>()
                .in(FunctionTool::getName, names)
                .eq(FunctionTool::getDeleted, 0));
    }

    @Select("SELECT * FROM function_tool WHERE category = #{category} AND deleted = 0")
    List<FunctionTool> selectByCategory(@Param("category") String category);

    /** 查询已启用的工具 (使用 LambdaQueryWrapper 避免 @Select 注解绑定问题) */
    default List<FunctionTool> selectEnabled() {
        return selectList(new LambdaQueryWrapper<FunctionTool>()
                .eq(FunctionTool::getEnabled, 1)
                .eq(FunctionTool::getDeleted, 0)
                .orderByDesc(FunctionTool::getCreatedAt));
    }
}
