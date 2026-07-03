package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.system.entity.SysMenuEntity;
import com.vibe.system.vo.SysMenuVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单权限 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuEntity> {

    /**
     * 查询全部菜单（按排序），返回 VO
     */
    List<SysMenuVO> selectAllMenuVo();

    /**
     * 按角色ID列表查询菜单（去重）
     */
    List<SysMenuVO> selectMenusByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 按用户ID查询权限标识列表（按钮 perms）
     */
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 按角色ID查询关联菜单ID列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
