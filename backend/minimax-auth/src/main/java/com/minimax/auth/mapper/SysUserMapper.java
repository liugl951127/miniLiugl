package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询用户的角色编码集合。用于鉴权与前端展示。
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 按用户名查询（含已逻辑删除的，用 admin 重置等场景）。
     */
    SysUser selectByUsername(@Param("username") String username);
}
