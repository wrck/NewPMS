package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.system.entity.SysRoleMenuEntity;
import com.vibe.system.vo.RoleSimpleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色菜单关联 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenuEntity> {

    /**
     * 删除角色的全部菜单关联（逻辑删除）
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除菜单的全部角色关联（逻辑删除）
     */
    int deleteByMenuId(@Param("menuId") Long menuId);

    /**
     * 按菜单ID查询关联角色列表（简要信息）
     */
    List<RoleSimpleVO> selectRolesByMenuId(@Param("menuId") Long menuId);

    /**
     * 按菜单ID查询关联角色ID列表
     */
    List<Long> selectRoleIdsByMenuId(@Param("menuId") Long menuId);
}
