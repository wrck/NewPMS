package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.system.entity.SysUserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRoleEntity> {

    /**
     * 物理删除某用户的全部角色关联（重新分配时使用，逻辑删除由 @TableLogic 处理）
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 统计角色下的用户数
     */
    long countByRoleId(@Param("roleId") Long roleId);
}
