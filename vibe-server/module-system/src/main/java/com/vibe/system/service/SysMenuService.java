package com.vibe.system.service;

import com.vibe.system.dto.SysMenuDTO;
import com.vibe.system.vo.SysMenuVO;

import java.util.List;

/**
 * 菜单权限服务
 *
 * @author vibe
 */
public interface SysMenuService {

    /**
     * 查询全部菜单（树形）
     */
    List<SysMenuVO> listTree();

    /**
     * 查询全部菜单（扁平列表）
     */
    List<SysMenuVO> listAll();

    Long create(SysMenuDTO dto);

    void update(SysMenuDTO dto);

    void delete(Long id);

    SysMenuVO getDetail(Long id);

    /**
     * 按角色ID查询菜单（树形）
     */
    List<SysMenuVO> getMenusByRoleId(Long roleId);

    /**
     * 按用户ID查询菜单（树形，用于前端动态路由）
     */
    List<SysMenuVO> getMenusByUserId(Long userId);

    /**
     * 按用户ID查询权限标识列表，供登录后获取用户权限。
     */
    List<String> getPermissionsByUserId(Long userId);

    /**
     * 按权限标识列表查询菜单ID列表。
     *
     * <p>用于角色权限分配时，将前端传入的 {@code permissionCodes}（权限标识或菜单ID字符串）
     * 转换为菜单ID列表。支持两种格式：
     * <ul>
     *   <li>数字字符串（如 ["1","2"]）—— 直接解析为 Long</li>
     *   <li>权限标识（如 ["system:user"]）—— 通过 perms 字段查询</li>
     * </ul>
     *
     * @param permissionCodes 权限标识列表
     * @return 菜单ID列表
     */
    List<Long> getMenuIdsByPermissionCodes(List<String> permissionCodes);
}
