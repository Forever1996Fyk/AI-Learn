package com.forever1996Fyk.ai.agentplus.sys.mapper;

import com.forever1996Fyk.ai.agentplus.sys.entity.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * sys_user_role Mapper（联合主键，不能用 selectById/updateById）。
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
