package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysRoleDTO;
import com.vibe.system.dto.SysRoleMenuDTO;
import com.vibe.system.dto.SysRoleQueryDTO;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysRoleVO;

import java.util.List;

/**
 * 系统角色服务
 *
 * @author vibe
 */
public interface SysRoleService {

    PageResult<SysRoleVO> page(SysRoleQueryDTO query);

    List<SysRoleVO> listAll();

    Long create(SysRoleDTO dto);

    void update(SysRoleDTO dto);

    void delete(Long id);

    SysRoleVO getDetail(Long id);

    /**
     * 分配角色菜单权限
     */
    void assignMenus(Long roleId, SysRoleMenuDTO dto);

    /**
     * 查询角色关联菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 按用户ID查询角色列表
     */
    List<RoleSimpleVO> getRolesByUserId(Long userId);

    /**
     * 按用户ID查询角色编码列表
     */
    List<String> getRoleCodesByUserId(Long userId);
}
