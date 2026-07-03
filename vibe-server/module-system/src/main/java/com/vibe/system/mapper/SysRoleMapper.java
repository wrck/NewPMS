package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysRoleQueryDTO;
import com.vibe.system.entity.SysRoleEntity;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysRoleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统角色 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {

    /**
     * 按用户ID查询角色列表（简要信息）
     */
    List<RoleSimpleVO> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID查询角色编码列表
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 分页查询角色（含关联菜单ID列表）
     */
    IPage<SysRoleVO> selectRolePage(IPage<SysRoleVO> page, @Param("query") SysRoleQueryDTO query);

    /**
     * 按角色ID查询关联菜单ID列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
