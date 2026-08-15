package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
    Tenant selectByCode(@Param("code") String code);
    List<Tenant> selectByOwner(@Param("ownerId") Long ownerId);
}
