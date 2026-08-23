package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("sys_user_role")
public class SysUserRole implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;
    @TableField("role_id")
    private Long roleId;
}
